(ns automaton-simulation-de.simulation-engine.impl.model-data
  "Model data is the data used as inputs to generate the model:

  * data oriented, so it could be persisted easily.
  * ergonomy oriented, so it is easy to use."
  (:require
   [automaton-simulation-de.simulation-engine                        :as-alias sim-engine]
   [automaton-simulation-de.simulation-engine.event                  :as sim-de-event]
   [automaton-simulation-de.simulation-engine.impl.stopping.criteria :as sim-de-criteria]
   [automaton-simulation-de.simulation-engine.middleware             :as sim-de-middleware]))

(def middlewares-schema
  [:sequential [:or :keyword sim-de-middleware/schema [:tuple :keyword] [:tuple :keyword :any]]])

(def stopping-criterias-schema
  [:sequential [:or :keyword sim-de-criteria/schema [:tuple :keyword] [:tuple :keyword [:map]]]])

(def ordering-schema
  [:sequential
   [:or
    [:tuple [:enum ::sim-engine/field] :keyword]
    [:tuple [:enum ::sim-engine/type] [:sequential :keyword]]]])

(def schema
  [:map {:closed false}
   [::sim-engine/initial-bucket {:optional true}
    :int]
   [::sim-engine/future-events {:optional true}
    [:vector sim-de-event/schema]]
   [::sim-engine/middlewares {:optional true}
    middlewares-schema]
   [::sim-engine/ordering {:optional true}
    ordering-schema]
   [::sim-engine/stopping-criterias {:optional true}
    stopping-criterias-schema]])
