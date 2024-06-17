(ns automaton-simulation-de.simulation-engine.impl.model
  "A simulation model gathers information required to run the simulation.

  It includes:

  * `initial-snpashot` snapshot to start the model with.
  * `middlewares` sequence of middleware to execute with the model.
  * `model-data` data version of this model.
  * `ordering` ordering of events.
  * `registry` registry.
  * `stopping-criterias` list of `stopping-criteria` that defines the end of the model."
  (:require
   [automaton-simulation-de.simulation-engine
    :as-alias sim-engine]
   [automaton-simulation-de.simulation-engine.impl.middleware.registry
    :as sim-de-middleware-registry]
   [automaton-simulation-de.simulation-engine.impl.middlewares
    :as sim-de-middlewares]
   [automaton-simulation-de.simulation-engine.impl.model-data
    :as sim-de-model-data]
   [automaton-simulation-de.simulation-engine.impl.registry
    :as sim-de-registry]
   [automaton-simulation-de.simulation-engine.impl.stopping.criteria
    :as sim-de-criteria]
   [automaton-simulation-de.simulation-engine.ordering
    :as sim-de-ordering]
   [automaton-simulation-de.simulation-engine.snapshot
    :as sim-de-snapshot]))

(def schema
  [:map {:closed false}
   [::sim-engine/initial-snapshot sim-de-snapshot/schema]
   [::sim-engine/middlewares sim-de-middlewares/schema]
   [::sim-engine/model-data sim-de-model-data/schema]
   [::sim-engine/ordering [:sequential sim-de-ordering/schema]]
   [::sim-engine/registry sim-de-registry/schema]
   [::sim-engine/stopping-criterias [:sequential sim-de-criteria/schema]]])

(defn build
  "Turns `model-data` into a `model`.
  Retuns the `model`'s."
  [{::sim-engine/keys
    [initial-event-type initial-bucket middlewares ordering stopping-criterias]
    :or {initial-bucket 0}
    :as model-data}
   registry]
  (let [updated-middlewares (->> middlewares
                                 (mapv (partial
                                        sim-de-middleware-registry/data-to-fn
                                        (::sim-engine/middleware registry))))
        updated-scs (->> stopping-criterias
                         (map (partial sim-de-criteria/api-data-to-entity
                                       (::sim-engine/stopping registry)))
                         (filter some?)
                         (mapv sim-de-criteria/model-end))
        updated-ordering (->> ordering
                              (map sim-de-ordering/data-to-fn)
                              (filterv some?))]
    #:automaton-simulation-de.simulation-engine{:registry registry
                                                :middlewares updated-middlewares
                                                :model-data model-data
                                                :stopping-criterias updated-scs
                                                :ordering updated-ordering
                                                :initial-snapshot
                                                (sim-de-snapshot/initial
                                                 initial-event-type
                                                 initial-bucket)}))
