(ns automaton-simulation-de.simulation-engine.impl.middlewares
  "Ordered list of middlewares.

  * [See entity](docs/archi/middlewares_entity.png)"
  (:require
   [automaton-core.adapters.schema                       :as core-schema]
   [automaton-core.utils.sequences                       :as core-sequences]
   [automaton-simulation-de.simulation-engine            :as-alias sim-engine]
   [automaton-simulation-de.simulation-engine.middleware :as sim-de-middleware]))

(def schema [:sequential sim-de-middleware/schema])

(defn wrap-handler
  "Wrap the `handler` with the middlewares.
  Returns the handler value wrapped in all the middlewares.

  Note that an handler is understood as a function taking a `request` and returning a `response`."
  [handler middlewares]
  (reduce (fn [handler middleware] (middleware handler)) handler middlewares))

(defn concat-supp-middlewares
  "Adds `supp-middlewares` in `middlewares` where `::sim-engine/supp-middlewares-insert` stands."
  [middlewares supp-middlewares]
  (core-sequences/concat-at middlewares ::sim-engine/supp-middlewares-insert supp-middlewares))

(defn validate [middlewares] (core-schema/validate-data-humanize schema middlewares))
