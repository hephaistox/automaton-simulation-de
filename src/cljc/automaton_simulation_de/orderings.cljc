(ns automaton-simulation-de.orderings
  "Sequence of `ordering`."
  (:require
   [automaton-core.adapters.schema   :as core-schema]
   [automaton-simulation-de.ordering :as sim-de-ordering]))

(def schema [:sequential sim-de-ordering/schema])

(defn validate
  [orderings]
  (core-schema/validate-data-humanize schema orderings))
