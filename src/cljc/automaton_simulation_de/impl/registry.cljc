(ns automaton-simulation-de.impl.registry
  "Gathers the registries of a simulation."
  (:require
   [automaton-core.adapters.schema                            :as core-schema]
   [automaton-simulation-de.event-registry
    :as sim-de-event-registry]
   [automaton-simulation-de.impl.middleware.registry
    :as sim-de-middleware-registry]
   [automaton-simulation-de.impl.ordering.registry
    :as sim-de-ordering-registry]
   [automaton-simulation-de.impl.stopping-definition.registry
    :as sim-de-stopping-registry]))

(def schema
  [:map {:closed true}
   [:stopping sim-de-stopping-registry/schema]
   [:middleware sim-de-middleware-registry/schema]
   [:event sim-de-event-registry/schema]
   [:ordering sim-de-ordering-registry/schema]])

(defn validate [registry] (core-schema/validate-data-humanize schema registry))

(defn build
  []
  (let [registry {:stopping (sim-de-stopping-registry/build)
                  :middleware (sim-de-middleware-registry/build)
                  :event {}
                  :ordering (sim-de-ordering-registry/build)}]
    (if-let [validation (validate registry)]
      (assoc validation :invalid? true)
      registry)))
