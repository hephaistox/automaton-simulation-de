(ns automaton-simulation-de.impl.built-in-sd.causality-broken
  "`stopping-definition` to stop when causality is broken.

  Causality is a property of the simulation model stating that a future event cannot change what has been done in the past already, so all changes in the state should not contradict any past event.

  In practise, we check the `next-snapshot` `date` is equal or after the current `snapshot`."
  (:require
   [automaton-simulation-de.scheduler.snapshot :as sim-de-snapshot]))

(def stopping-definition
  {:id :causality-broken
   :next-possible? true
   :doc "Stops when next snapshot is before current one."})

(defn evaluates
  [snapshot next-snapshot current-event]
  (when (pos? (compare (::sim-de-snapshot/date snapshot)
                       (::sim-de-snapshot/date next-snapshot)))
    {:stopping-criteria {:stopping-definition stopping-definition}
     :current-event current-event
     :context {:previous-date (::sim-de-snapshot/date snapshot)
               :next-date (::sim-de-snapshot/date next-snapshot)}}))
