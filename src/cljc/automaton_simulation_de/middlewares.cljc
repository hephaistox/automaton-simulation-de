(ns automaton-simulation-de.middlewares
  "Ordered list of middlewares

  * [See entity](docs/archi/middlewares_entity.png)"
  (:require
   [automaton-simulation-de.middleware :as sim-de-middleware]))

(defn schema [] [:sequential (sim-de-middleware/schema)])

(defn wrap-handler
  "Wrap the handler with the middlewares.

  Returns the handler value wrapped in all the middlewares.

  Params:
  * `handler` function taking a request and returning a response
  * `middlewares` that will wrap the handler"
  [handler middlewares]
  (reduce (fn [handler middleware] (middleware handler)) handler middlewares))
