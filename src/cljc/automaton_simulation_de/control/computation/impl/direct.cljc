(ns automaton-simulation-de.control.computation.impl.direct
  "Naive implementation of computation.
   It directly executes scheduler each time it's asked for an answer"
  (:require
   [automaton-simulation-de.control.computation          :as sim-de-computation]
   [automaton-simulation-de.control.computation.response :as
                                                         sim-de-comp-response]
   [automaton-simulation-de.simulation-engine            :as sim-engine]))

(defn- endless-sim?
  [stopping-causes]
  (some
   #(= :endless-sim
       (get-in % [::sim-engine/stopping-criteria ::sim-engine/params :reason]))
   stopping-causes))

(defn- stopping-criteria-match-cause?
  [stopping-criterias stop-causes]
  (if (empty? stopping-criterias)
    true
    (some (fn [stop-cause]
            (some #(and (= (get-in stop-cause
                                   [::sim-engine/stopping-criteria
                                    ::sim-engine/params])
                           (second %))
                        (= (get-in stop-cause
                                   [::sim-engine/stopping-criteria
                                    ::sim-engine/stopping-definition
                                    ::sim-engine/id])
                           (first %)))
                  stopping-criterias))
          stop-causes)))

(defrecord DirectComputation [model max-it]
  sim-de-computation/Computation
    (scheduler-response [_ stopping-criterias it]
      (let [it (or it 1)
            endless-sim-catch (+ it max-it)
            endless-stopping-criteria [::sim-engine/iteration-nth
                                       {::sim-engine/n endless-sim-catch
                                        :reason :endless-sim}]
            it-snapshot
            (if (<= it 1)
              (::sim-engine/initial-snapshot model)
              (-> model
                  (sim-engine/scheduler
                   []
                   [[::sim-engine/iteration-nth
                     #:automaton-simulation-de.simulation-engine{:n (dec it)}]])
                  ::sim-engine/snapshot))]
        (loop [snapshot it-snapshot
               endless-catch-internal-counter 0]
          (let [{::sim-engine/keys [snapshot stopping-causes]
                 :as resp}
                (sim-engine/scheduler model
                                      []
                                      (conj stopping-criterias
                                            endless-stopping-criteria)
                                      snapshot)]
            (cond
              (endless-sim? stopping-causes)
              (sim-de-comp-response/build :timeout resp)
              (stopping-criteria-match-cause? stopping-criterias
                                              stopping-causes)
              (sim-de-comp-response/build :success resp)
              (= endless-catch-internal-counter max-it)
              (sim-de-comp-response/build :internal-error resp)
              (sim-de-computation/not-next? stopping-causes)
              (sim-de-comp-response/build :no-next resp)
              :else (recur snapshot (inc endless-catch-internal-counter)))))))
    (scheduler-response [this]
      (sim-de-computation/scheduler-response this [] 1))
    (scheduler-response [this stopping-criterias]
      (sim-de-computation/scheduler-response this stopping-criterias 1))
    (stopping-criterias [_] (::sim-engine/stopping-criterias model)))

(defn make-direct-computation
  "Params:
   `model` simulation model
   `max-it` maximum number of iteration to go to"
  ([model max-it] (->DirectComputation model max-it))
  ([model] (make-direct-computation model 100)))
