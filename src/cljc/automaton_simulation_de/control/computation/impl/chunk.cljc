(ns automaton-simulation-de.control.computation.impl.chunk
  "Loads chunk of scheduler responses upfront"
  (:require
   [automaton-core.adapters.schema                                   :as core-schema]
   [automaton-simulation-de.control                                  :as-alias sim-de-control]
   [automaton-simulation-de.control.computation                      :as sim-de-computation]
   [automaton-simulation-de.control.computation.response             :as sim-de-comp-response]
   [automaton-simulation-de.simulation-engine                        :as sim-engine]
   [automaton-simulation-de.simulation-engine.impl.stopping.criteria :as sim-de-criteria]))

(defn create-storage
  ([model]
   (create-storage model
                   {1 #:automaton-simulation-de.simulation-engine{:stopping-causes []
                                                                  :snapshot
                                                                  (::sim-engine/initial-snapshot
                                                                   model)}}))
  ([model iterations]
   (atom {:model model
          :no-next false
          :iterations (into (sorted-map) iterations)})))

(defn- last-resp [buffer] (last (:iterations @buffer)))

(defn- highest-it [buffer] (or (first (last-resp buffer)) 0))

(defn- iteration-stp-crit
  [n]
  [::sim-engine/iteration-nth {::sim-engine/n n
                               :chunk? true}])

(defn- mdw-save-it
  [storage handler]
  (fn [request]
    (let [response (handler request)
          it-nb (get-in response [::sim-engine/snapshot ::sim-engine/iteration])]
      (swap! storage assoc-in
        [:iterations it-nb]
        (-> response
            (update ::sim-engine/stopping-causes
                    (fn [causes]
                      (remove (fn [cause]
                                (true? (:chunk? (::sim-engine/params (::sim-engine/stopping-criteria
                                                                      cause)))))
                              causes)))))
      response)))

(defn- go-to-stop!
  [buffer stp-criterias]
  (let [model (:model @buffer)
        endless-catch (iteration-stp-crit (+ (highest-it buffer) 10000))
        stp-criterias (conj stp-criterias endless-catch)]
    (->> buffer
         last-resp
         second
         ::sim-engine/snapshot
         (sim-engine/scheduler model [(partial mdw-save-it buffer)] stp-criterias))))

(defn- load-chunk!
  [storage chunk-size]
  (when (false? (:no-next @storage))
    (let [iteration-stop (iteration-stp-crit (+ (highest-it storage) chunk-size))
          last-snapshot (-> storage
                            last-resp
                            second
                            ::sim-engine/snapshot)]
      (sim-engine/scheduler (:model @storage)
                            [(partial mdw-save-it storage)]
                            [iteration-stop]
                            last-snapshot)
      (swap! storage assoc
        :no-next
        (-> storage
            last-resp
            second
            ::sim-engine/stopping-causes
            sim-de-computation/not-next?)))))

(defn check-predicates
  [stopping-criterias {::sim-engine/keys [snapshot stopping-causes]}]
  (let [user-stopping-causes (->> stopping-criterias
                                  (map (fn [stopping-criteria]
                                         (sim-de-criteria/evaluates stopping-criteria snapshot)))
                                  (remove nil?))]
    (cond
      (and (empty? stopping-criterias) (not-empty stopping-causes)) stopping-causes
      (not-empty user-stopping-causes) (concat stopping-causes user-stopping-causes)
      :else false)))

(defn- add-user-stopping-causes
  [stopping-criterias response]
  (assoc response ::sim-engine/stopping-causes (check-predicates stopping-criterias response)))

(defn- remove-user-stopping-causes
  [it]
  (update it
          ::sim-engine/stopping-causes
          (fn [causes]
            (filter #(nil?
                      (get-in % [::sim-engine/stopping-criteria ::sim-engine/params :chunk-user?]))
                    causes))))

(defrecord ChunkComputation [storage chunk-size]
  sim-de-computation/Computation
    (scheduler-response [this] (sim-de-computation/scheduler-response this [] 1))
    (scheduler-response [this stopping-criterias]
      (sim-de-computation/scheduler-response this stopping-criterias 1))
    (scheduler-response [_ stopping-criterias it]
      (let [it (or it 1)
            max-it (highest-it storage)
            stopping-registry (::sim-engine/stopping (::sim-engine/registry (:model @storage)))
            stopping-criterias
            (->> stopping-criterias
                 (map (partial sim-de-criteria/api-data-to-entity stopping-registry))
                 (filter some?)
                 (mapv sim-de-criteria/out-of-model)
                 (mapv (fn [stp-criteria]
                         (assoc-in stp-criteria [::sim-engine/params :chunk-user?] true))))
            stopping-criterias-valid? (->> stopping-criterias
                                           (map #(core-schema/validate-data sim-de-criteria/schema
                                                                            %))
                                           (every? true?))]
        (if (not stopping-criterias-valid?)
          (sim-de-comp-response/build
           :internal-error
           #:automaton-simulation-de.simulation-engine{:stopping-criterias stopping-criterias})
          (do
            (when (>= (+ it chunk-size) max-it) (load-chunk! storage (+ chunk-size (- it max-it))))
            (if-let [response (-> (fn [[k v]]
                                    (and (>= k it) (check-predicates stopping-criterias v)))
                                  (filter (:iterations @storage))
                                  first
                                  second)]
              (sim-de-comp-response/build :success
                                          (add-user-stopping-causes stopping-criterias response))
              (let [loop-start-iteration (second (last-resp storage))]
                (loop [{::sim-engine/keys [stopping-causes snapshot]
                        :as it}
                       loop-start-iteration
                       endless-catch-internal-counter 0]
                  (cond
                    (check-predicates stopping-criterias it)
                    (do (swap! storage assoc-in
                          [:iterations (get-in it [::sim-engine/snapshot ::sim-engine/iteration])]
                          (remove-user-stopping-causes it))
                        (load-chunk! storage (+ chunk-size (get snapshot ::sim-engine/iteration 0)))
                        (sim-de-comp-response/build :success
                                                    (add-user-stopping-causes stopping-criterias
                                                                              it)))
                    (sim-de-computation/not-next? stopping-causes)
                    (sim-de-comp-response/build :no-next it)
                    (or (= endless-catch-internal-counter 10000)
                        (>= (- (get snapshot ::sim-engine/iteration 0)
                               (get-in loop-start-iteration
                                       [::sim-engine/snapshot ::sim-engine/iteration]
                                       0))
                            10000))
                    (sim-de-comp-response/build :timeout it)
                    :else (do (go-to-stop! storage stopping-criterias)
                              (recur (second (last-resp storage))
                                     (inc endless-catch-internal-counter)))))))))))
    (stopping-criterias [_] (::sim-engine/stopping-criterias (:model @storage))))

(defn make-chunk-computation
  "`chunk-size` defines how many responses upfront to load"
  [model chunk-size]
  (->ChunkComputation (create-storage model) chunk-size))
