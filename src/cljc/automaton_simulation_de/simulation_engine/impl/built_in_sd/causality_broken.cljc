(ns automaton-simulation-de.simulation-engine.impl.built-in-sd.causality-broken
  "`stopping-definition` to stop when causality is broken.

  Causality is a property of the simulation model stating that a future event cannot change what has been done in the past already, so all changes in the state should not contradict any past event.

  In practise, we check the `next-snapshot` `date` is equal or after the current `snapshot`."
  (:require
   [automaton-simulation-de.simulation-engine :as-alias sim-engine]))

(def stopping-definition
  {::sim-engine/id ::sim-engine/causality-broken
   ::sim-engine/next-possible? true
   ::sim-engine/doc "Stops when next snapshot is before current one."})

(defn evaluates
  [snapshot next-snapshot current-event]
  (let [snapshot-date (::sim-engine/date snapshot)
        next-snapshot-date (::sim-engine/date next-snapshot)]
    (when (pos? (compare snapshot-date next-snapshot-date))
      #:automaton-simulation-de.simulation-engine{:stopping-criteria
                                                  #:automaton-simulation-de.simulation-engine{:stopping-definition
                                                                                              stopping-definition}
                                                  :current-event current-event
                                                  :context
                                                  #:automaton-simulation-de.simulation-engine{:previous-date
                                                                                              snapshot-date
                                                                                              :next-date
                                                                                              next-snapshot-date}})))
