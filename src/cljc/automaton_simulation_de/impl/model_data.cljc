(ns automaton-simulation-de.impl.model-data
  "Model data is the data used as inputs to generate the model:

  * data oriented, so it could be persisted easily.
  * ergonomy oriented, so it is easy to use."
  (:require
   [automaton-core.adapters.schema                 :as core-schema]
   [automaton-simulation-de.impl.stopping.criteria :as sim-de-criteria]
   [automaton-simulation-de.middleware             :as sim-de-middleware]))

(def middlewares-schema
  [:sequential
   [:or
    :keyword
    sim-de-middleware/schema
    [:tuple :keyword]
    [:tuple :keyword :any]]])

(def stopping-criterias-schema
  [:sequential
   [:or
    :keyword
    sim-de-criteria/schema
    [:tuple :keyword]
    [:tuple :keyword [:map]]]])

(def ordering-schema
  [:sequential
   [:or
    [:tuple [:enum :field] :keyword]
    [:tuple [:enum :type] [:sequential :keyword]]]])

(def model-data-schema
  [:map {:closed false}
   [:initial-event-type :keyword]
   [:initial-bucket {:optional true}
    :int]
   [:middlewares {:optional true}
    middlewares-schema]
   [:ordering {:optional true}
    ordering-schema]
   [:stopping-criterias {:optional true}
    stopping-criterias-schema]])

(defn validate-humanize
  [model-data]
  (core-schema/validate-data-humanize model-data-schema model-data))
