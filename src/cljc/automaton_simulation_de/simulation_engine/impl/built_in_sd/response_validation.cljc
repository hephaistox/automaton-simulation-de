(ns
  automaton-simulation-de.simulation-engine.impl.built-in-sd.response-validation
  "Stops when the response is not valid."
  (:require
   [automaton-simulation-de.simulation-engine :as-alias sim-engine]))

(defn stopping-definition
  []
  #:automaton-simulation-de.simulation-engine{:doc
                                              "Stops when the response is not valid."
                                              :id ::sim-engine/response-schema
                                              :next-possible? true})
