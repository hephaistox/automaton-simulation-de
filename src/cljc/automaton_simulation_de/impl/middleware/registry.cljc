(ns automaton-simulation-de.impl.middleware.registry
  "Registry for middlewares associating a keyword to its middleware function."
  (:require
   [automaton-core.adapters.schema                              :as core-schema]
   [automaton-simulation-de.impl.middleware.request-validation
    :as sim-de-mdw-request-validation]
   [automaton-simulation-de.impl.middleware.response-validation
    :as sim-de-mdw-response-validation]
   [automaton-simulation-de.impl.middleware.state-rendering
    :as sim-de-state-rendering]
   [automaton-simulation-de.impl.middleware.tapping             :as
                                                                sim-de-tapping]
   [automaton-simulation-de.middleware
    :as sim-de-middleware]))

(def schema [:map-of sim-de-middleware/id-schema sim-de-middleware/schema])

(defn build
  "Returns the registry of middlewares."
  []
  {:state-rendering sim-de-state-rendering/wrap
   :state-printing sim-de-state-rendering/wrap-print
   :tap-request sim-de-tapping/wrap-request
   :sim-de-mdw-request-inconsistency sim-de-mdw-request-validation/wrap-request
   :sim-de-mdw-response-validation sim-de-mdw-response-validation/wrap-response
   :tap-response sim-de-tapping/wrap-response})

(defn validate [registry] (core-schema/validate-data-humanize schema registry))

(defn data-to-fn
  "Turns a `middleware` - a data - to a middleware function."
  [middleware-registry middleware]
  (cond
    (= :supp-middlewares-insert middleware) :supp-middlewares-insert
    (= [:supp-middlewares-insert] middleware) :supp-middlewares-insert
    (keyword? middleware) (get middleware-registry middleware)
    (fn? middleware) middleware
    (sequential? middleware)
    (let [[middleware-name & middleware-params] middleware]
      (when-let [middleware-fn (get middleware-registry middleware-name)]
        (apply partial middleware-fn middleware-params)))))
