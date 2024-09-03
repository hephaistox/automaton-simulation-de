(ns automaton-simulation-de.simulation-engine.impl.middleware.request-validation
  "Stops when the request is valid through inconsistency and schema.
  This criteria is built-in to this middleware as it is requiring the `request` knowledge. User `stopping-criteria` knows only `snapshot`."
  (:require
   [automaton-core.adapters.schema                                                :as core-schema]
   [automaton-simulation-de.simulation-engine                                     :as-alias
                                                                                  sim-engine]
   [automaton-simulation-de.simulation-engine.impl.built-in-sd.request-validation
    :as sim-de-request-validation]
   [automaton-simulation-de.simulation-engine.request                             :as
                                                                                  sim-de-request]
   [automaton-simulation-de.simulation-engine.snapshot                            :as
                                                                                  sim-de-snapshot]))

(defn evaluates
  [{::sim-engine/keys [snapshot current-event]
    :as request}]
  (let [request-inconsistency (sim-de-snapshot/inconsistency? snapshot)
        request-inconsistency? (not (false? request-inconsistency))
        request-schema-error (core-schema/validate-data-humanize sim-de-request/schema request)
        request-error? (some? request-schema-error)]
    (when (or request-inconsistency? request-error?)
      (cond-> {::sim-engine/stopping-criteria {::sim-engine/stopping-definition
                                               (sim-de-request-validation/stopping-definition)}
               ::sim-engine/current-event current-event
               ::sim-engine/context {:request request}}
        request-inconsistency? (assoc-in [::sim-engine/context :inconsistency]
                                request-inconsistency)
        request-error? (assoc-in [::sim-engine/context :schema] request-schema-error)))))

(defn wrap-request
  [handler]
  (fn [request]
    (-> request
        (sim-de-request/add-stopping-cause (evaluates request))
        handler)))
