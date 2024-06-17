(ns
  automaton-simulation-de.simulation-engine.impl.stopping-definition.state-contains
  (:require
   [automaton-simulation-de.simulation-engine :as-alias sim-engine]))

(defn stop?
  "Is the `snapshot` state contains value under params `state`. Considers empty collection as no value"
  [snapshot
   {:keys [state]
    :as _params}]
  (let [snapshot-state (get snapshot ::sim-engine/state {})
        state-entry (get-in snapshot-state state)]
    #:automaton-simulation-de.simulation-engine{:stop? (if (coll? state-entry)
                                                         (not-empty state-entry)
                                                         (some? state-entry))
                                                :context
                                                #:automaton-simulation-de.simulation-engine{:snapshot-state
                                                                                            snapshot-state}}))

(defn stopping-definition
  []
  #:automaton-simulation-de.simulation-engine{:doc
                                              "Stops when `state` path is containing any value"
                                              :id :state-contains
                                              :next-possible? true
                                              :stopping-evaluation stop?})
