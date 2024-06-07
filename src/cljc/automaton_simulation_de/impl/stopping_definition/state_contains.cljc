(ns automaton-simulation-de.impl.stopping-definition.state-contains
  (:require
   [automaton-simulation-de.scheduler.snapshot :as sim-de-snapshot]))

(defn stop?
  "Is the `snapshot` state contains value under params `state`. Considers empty collection as no value"
  [snapshot
   {:keys [state]
    :as _params}]
  (let [snapshot-state (get snapshot ::sim-de-snapshot/state {})
        state-entry (get-in snapshot-state state)]
    {:stop? (if (coll? state-entry) (not-empty state-entry) (some? state-entry))
     :context {:snapshot-state snapshot-state}}))

(defn stopping-definition
  []
  {:doc "Stops when `state` path is containing any value"
   :id :state-contains
   :next-possible? true
   :stopping-evaluation stop?})
