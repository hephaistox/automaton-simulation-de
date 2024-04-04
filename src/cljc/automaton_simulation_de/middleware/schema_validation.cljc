(ns automaton-simulation-de.middleware.schema-validation
  "Used to validate both request and response
  The inconsistency of snapshots in both request and response are also detected"
  (:require
   [automaton-core.adapters.schema              :as core-schema]
   [automaton-simulation-de.middleware.request  :as sim-de-request]
   [automaton-simulation-de.middleware.response :as sim-de-response]
   [automaton-simulation-de.scheduler.snapshot  :as sim-de-snapshot]))

(defn request-validation
  "Validate the request is well formed

  Check the request's value is compatible with the scherma and its snapshot is consistent

  Stop could be `::request-inconsistency` or `::request-schema`

  Params:
  * `handler`"
  [handler]
  (fn [request]
    (let [request-error
          (core-schema/validate-data-humanize (sim-de-request/schema) request)
          request-inconsistency (sim-de-snapshot/inconsistency?
                                 (::sim-de-request/snapshot request))]
      (-> (cond->
            request (some? request-error)
            (sim-de-request/add-stop {:cause ::request-schema
                                      :request request
                                      :error request-error})
            (not (false? request-inconsistency))
            (sim-de-response/add-stop {:cause ::request-inconsistency
                                       :request request
                                       :error request-inconsistency}))
          handler))))

(defn response-validation
  "Validate the response is well formed

  Check the response's value is compatible with the schema and its snapshot is consistent

  Stop could be `::response-inconsistency` or `::response-schema`

  Params:
  * `handler`"
  [handler]
  (fn [request]
    (let [response (handler request)
          response-error
          (core-schema/validate-data-humanize (sim-de-response/schema) response)
          response-inconsistency (sim-de-snapshot/inconsistency?
                                  (::sim-de-response/snapshot response))]
      (cond-> response
        (some? response-error) (update ::sim-de-response/stop
                                       conj
                                       {:cause ::response-schema
                                        :response response
                                        :error response-error})
        (not (false? response-inconsistency))
        (sim-de-response/add-stop {:cause ::response-inconsistency
                                   :response response
                                   :error response-inconsistency})))))
