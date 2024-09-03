(ns automaton-simulation-de.simulation-engine
  "Simulation is a technique that mimics a real system - and simplifies it, to learn something useful about it.
Discrete event simulation is modeling a real system with discrete events.

* Contains a user-specific domain, constraints, a customer-specific state and events modeling and an option to render visually the effects.
* Customer simulation can use directly `DE Simulation` or a library that eases the modeling: `rc modeling`, `industry modeling`, … "
  (:require
   [automaton-core.adapters.schema                            :as core-schema]
   [automaton-simulation-de.simulation-engine.impl.model      :as sim-de-model]
   [automaton-simulation-de.simulation-engine.impl.model-data :as sim-de-model-data]
   [automaton-simulation-de.simulation-engine.impl.registry   :as sim-de-registry]
   [automaton-simulation-de.simulation-engine.impl.scheduler  :as sim-de-scheduler]
   [automaton-simulation-de.simulation-engine.response        :as sim-de-response]))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn registries "Returns the `built-in` registries of simulation-de." [] (sim-de-registry/build))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn validate-registry
  [registry]
  (core-schema/validate-data-humanize sim-de-registry/schema registry))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn validate-model-data
  "Validate `model-data`."
  [model-data]
  (core-schema/validate-data-humanize sim-de-model-data/schema model-data))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn build-model
  "Build the simulation model from `model-data` with `registry`.
  `registry` is optional and is defaulted to the `registries` fn."
  ([model-data] (sim-de-model/build model-data (registries)))
  ([model-data registry] (sim-de-model/build model-data registry)))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn validate-model
  "Model - as built with `build-model` - are validated."
  [model]
  (core-schema/validate-data-humanize sim-de-model/schema model))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn validate-middleware-data
  "Middleware data are validated."
  [middleware-data _registries]
  (core-schema/validate-data-humanize sim-de-model-data/middlewares-schema middleware-data))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn validate-stopping-criteria-data
  [stopping-criteria-data _registries]
  (core-schema/validate-data-humanize sim-de-model-data/stopping-criterias-schema
                                      stopping-criteria-data))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn scheduler
  "Scheduler is running the simulation described in the `model`.
   There are three arities for this function.
   * First one specificies the `model` only.
   * Second one adds `stopping-criteria` and supplementary middlewares (i.e. `supp-middelwares`).
   * Third one specify also a different `snapshot` to start with (supersedes the `initial-snapshot` in the `model`).

   Returns a `response` containing:
   * simulation `snapshot` of the last event.
   * `stopping-causes`, see [[automaton-simulation-de.impl.stopping-definition.registry]] for possible values."
  ([{::keys [initial-snapshot]
     :as model}]
   (scheduler model [] [] initial-snapshot))
  ([{::keys [initial-snapshot]
     :as model}
    scheduler-middlewares
    scheduler-stopping-criterias]
   (scheduler model scheduler-middlewares scheduler-stopping-criterias initial-snapshot))
  ([model scheduler-middlewares scheduler-stopping-criterias snapshot]
   (when-not (sim-de-scheduler/invalid-inputs model
                                              scheduler-middlewares
                                              scheduler-stopping-criterias
                                              snapshot)
     (sim-de-scheduler/scheduler model
                                 scheduler-middlewares
                                 scheduler-stopping-criterias
                                 snapshot))))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn extract-snapshot
  "Extract the `snapshot` of a `response`."
  [{::keys [snapshot]
    :as _response}]
  snapshot)

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn validate-response
  [response]
  (core-schema/validate-data-humanize sim-de-response/schema response))
