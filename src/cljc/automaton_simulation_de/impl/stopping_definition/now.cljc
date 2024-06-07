(ns automaton-simulation-de.impl.stopping-definition.now
  "`stopping-definition` to stop right now.")

(defn stop-now
  "Stops now."
  [_snapshot _params]
  {:stop? true
   :context nil})

(defn stopping-definition
  []
  {:doc "Criteria to stop right now."
   :id :stop-now
   :next-possible? true
   :stopping-evaluation stop-now})
