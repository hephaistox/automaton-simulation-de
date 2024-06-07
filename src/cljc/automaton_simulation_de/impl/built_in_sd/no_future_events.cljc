(ns automaton-simulation-de.impl.built-in-sd.no-future-events
  "`stopping-definition` to stop when no future events exists anymore."
  (:require
   [automaton-simulation-de.request :as sim-de-request]))

(def stopping-definition
  {:id :no-future-events
   :next-possible? false
   :doc "Stops when no future events exists anymore."})

(defn evaluates
  [request future-events]
  (cond-> request
    (empty? future-events) (sim-de-request/add-stopping-cause
                            {:stopping-criteria {:stopping-definition
                                                 stopping-definition}})))
