(ns automaton-simulation-de.simulation-engine.impl.stopping-definition.now
  "`stopping-definition` to stop right now."
  (:require
   [automaton-simulation-de.simulation-engine :as-alias sim-engine]))

(defn stop-now
  "Stops now."
  [_snapshot _params]
  #:automaton-simulation-de.simulation-engine{:stop? true
                                              :context nil})

(defn stopping-definition
  []
  #:automaton-simulation-de.simulation-engine{:doc "Criteria to stop right now."
                                              :id ::sim-engine/stop-now
                                              :next-possible? true
                                              :stopping-evaluation stop-now})
