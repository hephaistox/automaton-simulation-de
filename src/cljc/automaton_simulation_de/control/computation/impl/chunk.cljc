(ns automaton-simulation-de.control.computation.impl.chunk
  "Loads chunk of scheduler responses upfront"
  (:require
   [automaton-simulation-de.control.computation          :as sim-de-computation]
   [automaton-simulation-de.control.computation.response :as
                                                         sim-de-comp-response]
   [automaton-simulation-de.core                         :as simulation-core]
   [automaton-simulation-de.impl.stopping.criteria       :as sim-de-criteria]
   [automaton-simulation-de.response                     :as sim-de-response]))

(defn create-storage
  ([model]
   (create-storage model
                   {1 (sim-de-response/build
                       []
                       (:automaton-simulation-de.impl.model/initial-snapshot
                        model))}))
  ([model iterations]
   (atom {:model model
          :no-next false
          :iterations (into (sorted-map) iterations)})))

(defn- last-resp [buffer] (last (:iterations @buffer)))

(defn- highest-it [buffer] (or (first (last-resp buffer)) 0))

(defn- iteration-stp-crit
  [n]
  [:iteration-nth {:n n
                   :chunk? true}])

(defn- mdw-save-it
  [storage handler]
  (fn [request]
    (let [response (handler request)
          it-nb (get-in
                 response
                 [:automaton-simulation-de.response/snapshot
                  :automaton-simulation-de.scheduler.snapshot/iteration])]
      (swap! storage assoc-in
        [:iterations it-nb]
        (-> response
            (update :automaton-simulation-de.response/stopping-causes
                    (fn [causes]
                      (remove (fn [cause]
                                (true? (:chunk? (:params (:stopping-criteria
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
         :automaton-simulation-de.response/snapshot
         (simulation-core/scheduler model
                                    [(partial mdw-save-it buffer)]
                                    stp-criterias))))



(defn- load-chunk!
  [storage chunk-size]
  (when (false? (:no-next @storage))
    (let [iteration-stop (iteration-stp-crit (+ (highest-it storage)
                                                chunk-size))
          last-snapshot (-> storage
                            last-resp
                            second
                            :automaton-simulation-de.response/snapshot)]
      (simulation-core/scheduler (:model @storage)
                                 [(partial mdw-save-it storage)]
                                 [iteration-stop]
                                 last-snapshot)
      (swap! storage assoc
        :no-next
        (-> storage
            last-resp
            second
            ::sim-de-response/stopping-causes
            sim-de-computation/not-next?)))))

(defn check-predicates
  [stopping-criterias {:automaton-simulation-de.response/keys
                       [snapshot stopping-causes]}]
  (let [user-stopping-causes
        (->> stopping-criterias
             (map (fn [stopping-criteria]
                    (sim-de-criteria/evaluates stopping-criteria snapshot)))
             (remove nil?))]
    (cond
      (and (empty? stopping-criterias) (not-empty stopping-causes))
      stopping-causes
      (not-empty user-stopping-causes) (concat stopping-causes
                                               user-stopping-causes)
      :else false)))

(defn- add-user-stopping-causes
  [stopping-criterias response]
  (assoc response
         :automaton-simulation-de.response/stopping-causes
         (check-predicates stopping-criterias response)))

(defn- remove-user-stopping-causes
  [it]
  (update it
          :automaton-simulation-de.response/stopping-causes
          (fn [causes]
            (filter #(nil? (get-in % [:stopping-criteria :params :chunk-user?]))
                    causes))))

(defrecord ChunkComputation [storage chunk-size]
  sim-de-computation/Computation
    (scheduler-response [this]
      (sim-de-computation/scheduler-response this [] 1))
    (scheduler-response [this stopping-criterias]
      (sim-de-computation/scheduler-response this stopping-criterias 1))
    (scheduler-response [_ stopping-criterias it]
      (let [it (or it 1)
            max-it (highest-it storage)
            stopping-registry (:stopping
                               (:automaton-simulation-de.impl.model/registry
                                (:model @storage)))
            stopping-criterias
            (->> stopping-criterias
                 (map (partial sim-de-criteria/api-data-to-entity
                               stopping-registry))
                 (filter some?)
                 (mapv sim-de-criteria/out-of-model)
                 (mapv (fn [stp-criteria]
                         (assoc-in stp-criteria [:params :chunk-user?] true))))
            stopping-criterias-valid? (->> stopping-criterias
                                           (map sim-de-criteria/validate)
                                           (every? nil?))]
        (if (not stopping-criterias-valid?)
          (sim-de-comp-response/build :internal-error
                                      {:stopping-criterias stopping-criterias})
          (do
            (when (>= (+ it chunk-size) max-it)
              (load-chunk! storage (+ chunk-size (- it max-it))))
            (if-let [response (-> (fn [[k v]]
                                    (and (>= k it)
                                         (check-predicates stopping-criterias
                                                           v)))
                                  (filter (:iterations @storage))
                                  first
                                  second)]
              (sim-de-comp-response/build
               :success
               (add-user-stopping-causes stopping-criterias response))
              (let [loop-start-iteration (second (last-resp storage))]
                (loop [{:automaton-simulation-de.response/keys [stopping-causes
                                                                snapshot]
                        :as it}
                       loop-start-iteration
                       endless-catch-internal-counter 0]
                  (cond
                    (check-predicates stopping-criterias it)
                    (do
                      (swap! storage assoc-in
                        [:iterations
                         (get-in
                          it
                          [:automaton-simulation-de.response/snapshot
                           :automaton-simulation-de.scheduler.snapshot/iteration])]
                        (remove-user-stopping-causes it))
                      (load-chunk!
                       storage
                       (+ chunk-size
                          (get
                           snapshot
                           :automaton-simulation-de.scheduler.snapshot/iteration
                           0)))
                      (sim-de-comp-response/build
                       :success
                       (add-user-stopping-causes stopping-criterias it)))
                    (sim-de-computation/not-next? stopping-causes)
                    (sim-de-comp-response/build :no-next it)
                    (or
                     (= endless-catch-internal-counter 10000)
                     (>=
                      (-
                       (get
                        snapshot
                        :automaton-simulation-de.scheduler.snapshot/iteration
                        0)
                       (get-in
                        loop-start-iteration
                        [:automaton-simulation-de.response/snapshot
                         :automaton-simulation-de.scheduler.snapshot/iteration]
                        0))
                      10000))
                    (sim-de-comp-response/build :timeout it)
                    :else (do (go-to-stop! storage stopping-criterias)
                              (recur (second (last-resp storage))
                                     (inc
                                      endless-catch-internal-counter)))))))))))
    (stopping-criterias [_]
      (:automaton-simulation-de.impl.model/stopping-criterias (:model
                                                               @storage))))

(defn make-chunk-computation
  "`chunk-size` defines how many responses upfront to load"
  [model chunk-size]
  (->ChunkComputation (create-storage model) chunk-size))
