(ns automaton-simulation-de.impl.model
  "A simulation model gathers information required to run the simulation."
  (:require
   [automaton-simulation-de.impl.middlewares   :as sim-de-middlewares]
   [automaton-simulation-de.ordering           :as sim-de-ordering]
   [automaton-simulation-de.registry           :as sim-de-registry]
   [automaton-simulation-de.scheduler.snapshot :as sim-de-snapshot]))

(defn schema
  []
  [:map {:closed false}
   [::registry (sim-de-registry/schema)]
   [::middlewares (sim-de-middlewares/schema)]
   [::ordering [:sequential (sim-de-ordering/schema)]]
   [::initial-snapshot (sim-de-snapshot/schema)]
   [::max-iteration :int]])

(defn build
  "Builds a model."
  [registry middlewares ordering initial-snapshot max-iteration]
  {::registry registry
   ::middlewares middlewares
   ::ordering ordering
   ::initial-snapshot initial-snapshot
   ::max-iteration max-iteration})
