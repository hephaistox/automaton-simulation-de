(ns automaton-simulation-de.impl.ordering.registry
  "Registry for ordering."
  (:require
   [automaton-simulation-de.ordering :as sim-de-ordering]))

(def schema [:map])

(defn build
  []
  {:compare-field {:comparison-fn sim-de-ordering/compare-field}
   :compare-types {:comparison-fn sim-de-ordering/compare-types}})

