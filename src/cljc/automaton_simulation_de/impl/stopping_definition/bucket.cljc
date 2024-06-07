(ns automaton-simulation-de.impl.stopping-definition.bucket
  "`stopping-definition` to stop at a given bucket."
  (:require
   [automaton-simulation-de.scheduler.snapshot :as sim-de-snapshot]))

(defn stop-bucket
  "Stops after the `date` is reached, or after."
  [{::sim-de-snapshot/keys [date]
    :as _snapshot
    :or {date 0}}
   {:keys [b]
    :as _params}]
  (when (or (nil? b) (>= date b))
    {:stop? true
     :context nil}))

(defn stopping-definition
  []
  {:doc "Stop at `bucket` `b` or later on."
   :id :bucket
   :next-possible? true
   :stopping-evaluation stop-bucket})
