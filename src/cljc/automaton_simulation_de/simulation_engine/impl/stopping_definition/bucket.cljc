(ns automaton-simulation-de.simulation-engine.impl.stopping-definition.bucket
  "`stopping-definition` to stop at a given bucket."
  (:require
   [automaton-simulation-de.simulation-engine :as-alias sim-engine]))

(defn stop-bucket
  "Stops after the `date` is reached, or after."
  [{::sim-engine/keys [date]
    :as _snapshot
    :or {date 0}}
   {:keys [b]
    :as _params}]
  (when (or (nil? b) (>= date b))
    #:automaton-simulation-de.simulation-engine{:stop? true
                                                :context nil}))

(defn stopping-definition
  []
  #:automaton-simulation-de.simulation-engine{:doc
                                              "Stop at `bucket` `b` or later on."
                                              :id ::bucket-stopping
                                              :next-possible? true
                                              :stopping-evaluation stop-bucket})
