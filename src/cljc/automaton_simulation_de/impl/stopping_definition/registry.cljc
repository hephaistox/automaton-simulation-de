(ns automaton-simulation-de.impl.stopping-definition.registry
  "Stopping registry contains all `stopping-definition`.

  ![Entities](archi/simulation_engine/stopping_registry.png)"
  (:require
   [automaton-core.adapters.schema                                 :as
                                                                   core-schema]
   [automaton-core.utils.map                                       :as
                                                                   utils-map]
   [automaton-simulation-de.impl.built-in-sd.request-validation
    :as sim-de-request-validation]
   [automaton-simulation-de.impl.built-in-sd.response-validation
    :as sim-de-response-validation]
   [automaton-simulation-de.impl.stopping-definition.bucket
    :as sim-de-sc-bucket]
   [automaton-simulation-de.impl.stopping-definition.iteration-nth
    :as sim-de-sc-iteration-nth]
   [automaton-simulation-de.impl.stopping-definition.now
    :as sim-de-sc-now]
   [automaton-simulation-de.impl.stopping.definition
    :as sim-de-sc-definition]))

(def schema
  [:map-of sim-de-sc-definition/id-schema sim-de-sc-definition/schema])

(defn build
  "The registered stopping criteria are:

  * `:bucket` Stop at `bucket` `b` or later on.
  * `:iteration-nth` Stops when the iteration `n` is reached.
  * `:request-schema` Stops when the request is not valid.
  * `:response-schema` Stops when the response is not valid.
  * `:stop-now` Criteria to stop right now."
  []
  (->> (utils-map/maps-to-key [(sim-de-sc-iteration-nth/stopping-definition)
                               (sim-de-response-validation/stopping-definition)
                               (sim-de-sc-bucket/stopping-definition)
                               (sim-de-request-validation/stopping-definition)
                               (sim-de-sc-now/stopping-definition)]
                              :id)
       (core-schema/add-default schema)))

(comment
  ;; Execute in clj repl to generate build docstring.
  #?(:clj (->> (build)
               (map (fn [[k v]] (format "  * `%s` %s\n" k (:doc v))))
               sort
               (apply str)))
  ;
)
