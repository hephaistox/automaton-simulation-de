(ns automaton-simulation-de.impl.middleware.response-validation
  "Stops when the response is valid through inconsistency and schema.
  This criteria is built-in to this middleware as it is requiring the `response` knowledge. User `stopping-criteria` knows only `snapshot`."
  (:require
   [automaton-core.adapters.schema                               :as
                                                                 core-schema]
   [automaton-simulation-de.impl.built-in-sd.response-validation
    :as sim-de-response-validation]
   [automaton-simulation-de.request                              :as
                                                                 sim-de-request]
   [automaton-simulation-de.response
    :as sim-de-response]
   [automaton-simulation-de.scheduler.snapshot
    :as sim-de-snapshot]))

(defn evaluates
  [{::sim-de-response/keys [snapshot]
    :as response}
   current-event]
  (let [response-inconsistency (sim-de-snapshot/inconsistency? snapshot)
        response-inconsistency? (not (false? response-inconsistency))
        response-schema-error
        (core-schema/validate-data-humanize sim-de-response/schema response)
        response-error? (some? response-schema-error)]
    (when (or response-inconsistency? response-error?)
      (cond-> {:stopping-criteria
               {:stopping-definition
                (sim-de-response-validation/stopping-definition)}
               :current-event current-event
               :context {:response response}}
        response-inconsistency? (assoc-in [:context :inconsistency]
                                 response-inconsistency)
        response-error? (assoc-in [:context :schema] response-schema-error)))))

(defn wrap-response
  [handler]
  (fn [{::sim-de-request/keys [current-event]
        :as request}]
    (let [response (handler request)]
      (sim-de-response/add-stopping-cause response
                                          (evaluates response current-event)))))
