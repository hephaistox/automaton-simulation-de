(ns automaton-simulation-de.middleware
  "Defines a middleware, wrapping an handler to prepare the request before the handler execution, and modify the response after handler execution.
  Note that a middleware wrapping an handler returns a new handler, that can be wrapped again.

  *  [See entity](docs/archi/middleware_entity.png)"
  (:require
   [automaton-simulation-de.middleware.request           :as sim-de-request]
   [automaton-simulation-de.middleware.schema-validation
    :as sim-de-schema-validation]
   [automaton-simulation-de.middleware.tapping           :as sim-de-tapping]))

(defn schema [] [:=> [:cat (sim-de-request/schema)] :map])

(defn response-validation-mdw
  "Adds this middleware to validate the response of the `event-execution`.
  This middleware should be first in the list of middlewares to catch all modifications of the `response`.
  Stop could be `::response-inconsistency` or `::response-schema`."
  [handler]
  (sim-de-schema-validation/response-validation handler))

(defn request-validation-mdw
  "Adds this middleware to validate the response of the `event-execution`.
  This middleware should be last in the list of middlewares to catch all modifications of the `request`.
  Stop could be `::response-inconsistency` or `::response-schema`."
  [handler]
  (sim-de-schema-validation/response-validation handler))

(defn wrap-response
  "Taps the response."
  [handler]
  (sim-de-tapping/wrap-response handler))

(defn wrap-request
  "Wraps the request."
  [handler]
  (sim-de-tapping/wrap-request handler))
