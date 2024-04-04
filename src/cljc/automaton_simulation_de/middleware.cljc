(ns automaton-simulation-de.middleware
  "Defines a middleware, wrapping an handler to prepare the request before the handler execution, and modify the response after handler execution.
  Note that a middleware wrapping an handler returns a new handler, that can be wrapped again.

  *  [See entity](docs/archi/middleware_entity.png)"
  (:require
   [automaton-simulation-de.middleware.request :as sim-de-request]))

(defn schema [] [:=> [:cat (sim-de-request/schema)] :map])
