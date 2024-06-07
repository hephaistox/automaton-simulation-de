(ns automaton-simulation-de.impl.model
  "A simulation model gathers information required to run the simulation.

  It includes:

  * `initial-snpashot` snapshot to start the model with.
  * `middlewares` sequence of middleware to execute with the model.
  * `model-data` data version of this model.
  * `ordering` ordering of events.
  * `registry` registry.
  * `stopping-criterias` list of `stopping-criteria` that defines the end of the model."
  (:require
   [automaton-core.adapters.schema                   :as core-schema]
   [automaton-simulation-de.impl.middleware.registry :as
                                                     sim-de-middleware-registry]
   [automaton-simulation-de.impl.middlewares         :as sim-de-middlewares]
   [automaton-simulation-de.impl.model-data          :as sim-de-model-data]
   [automaton-simulation-de.impl.registry            :as sim-de-registry]
   [automaton-simulation-de.impl.stopping.criteria   :as sim-de-criteria]
   [automaton-simulation-de.ordering                 :as sim-de-ordering]
   [automaton-simulation-de.scheduler.snapshot       :as sim-de-snapshot]))

(def schema
  [:map {:closed false}
   [::initial-snapshot sim-de-snapshot/schema]
   [::middlewares sim-de-middlewares/schema]
   [::model-data sim-de-model-data/model-data-schema]
   [::ordering [:sequential sim-de-ordering/schema]]
   [::registry sim-de-registry/schema]
   [::stopping-criterias [:sequential sim-de-criteria/schema]]])

(defn validate [model] (core-schema/validate-data-humanize schema model))

(defn build
  "Turns `model-data` into a `model`.
  Retuns the `model`'s."
  [{:keys
    [initial-event-type initial-bucket middlewares ordering stopping-criterias]
    :or {initial-bucket 0}
    :as model-data}
   registry]
  (let [updated-middlewares (->> middlewares
                                 (mapv (partial
                                        sim-de-middleware-registry/data-to-fn
                                        (:middleware registry))))
        updated-scs (->> stopping-criterias
                         (map (partial sim-de-criteria/api-data-to-entity
                                       (:stopping registry)))
                         (filter some?)
                         (mapv sim-de-criteria/model-end))
        updated-ordering (->> ordering
                              (map sim-de-ordering/data-to-fn)
                              (filterv some?))]
    {::registry registry
     ::middlewares updated-middlewares
     ::model-data model-data
     ::stopping-criterias updated-scs
     ::ordering updated-ordering
     ::initial-snapshot (sim-de-snapshot/initial initial-event-type
                                                 initial-bucket)}))
