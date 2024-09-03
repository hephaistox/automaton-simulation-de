(ns automaton-simulation-de.simulation-engine.impl.ordering.registry
  "Registry for ordering."
  (:require
   [automaton-simulation-de.simulation-engine          :as-alias sim-engine]
   [automaton-simulation-de.simulation-engine.ordering :as sim-de-ordering]))

(def schema [:map])

(defn build
  []
  #:automaton-simulation-de.simulation-engine{:compare-field {:comparison-fn
                                                              sim-de-ordering/compare-field}
                                              :compare-types {:comparison-fn
                                                              sim-de-ordering/compare-types}})
