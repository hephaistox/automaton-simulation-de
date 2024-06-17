(ns automaton-simulation-de.simulation-engine.impl.built-in-sd.no-future-events
  "`stopping-definition` to stop when no future events exists anymore."
  (:require
   [automaton-simulation-de.simulation-engine         :as-alias sim-engine]
   [automaton-simulation-de.simulation-engine.request :as sim-de-request]))

(def stopping-definition
  #:automaton-simulation-de.simulation-engine{:id ::sim-engine/no-future-events
                                              :next-possible? false
                                              :doc
                                              "Stops when no future events exists anymore."})

(defn evaluates
  [request future-events]
  (cond-> request
    (empty? future-events)
    (sim-de-request/add-stopping-cause
     #:automaton-simulation-de.simulation-engine{:stopping-criteria
                                                 #:automaton-simulation-de.simulation-engine{:stopping-definition
                                                                                             stopping-definition}})))
