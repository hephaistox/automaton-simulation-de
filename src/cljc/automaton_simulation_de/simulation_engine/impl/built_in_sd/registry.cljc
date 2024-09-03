(ns automaton-simulation-de.simulation-engine.impl.built-in-sd.registry
  "built-in `stopping-definition` can create `stopping-cause` but they are not accessible for modellers, they are hard coded."
  (:require
   [automaton-core.adapters.schema                                                    :as
                                                                                      core-schema]
   [automaton-core.utils.map                                                          :as utils-map]
   [automaton-simulation-de.simulation-engine                                         :as-alias
                                                                                      sim-engine]
   [automaton-simulation-de.simulation-engine.impl.built-in-sd.causality-broken
    :as sim-de-causality-broken]
   [automaton-simulation-de.simulation-engine.impl.built-in-sd.execution-not-found
    :as sim-de-execution-not-found]
   [automaton-simulation-de.simulation-engine.impl.built-in-sd.failed-event-execution
    :as sim-failed-event-execution]
   [automaton-simulation-de.simulation-engine.impl.built-in-sd.no-future-events
    :as sim-de-no-future-events]
   [automaton-simulation-de.simulation-engine.impl.stopping.definition
    :as sim-de-sc-definition]))

(def schema [:map-of sim-de-sc-definition/id-schema sim-de-sc-definition/schema])

(def stopping-definitions
  [sim-de-causality-broken/stopping-definition
   sim-de-execution-not-found/stopping-definition
   sim-failed-event-execution/stopping-definition
   sim-de-no-future-events/stopping-definition])

(defn build
  " The built-in stopping definition"
  []
  (->> (utils-map/maps-to-key stopping-definitions ::sim-engine/id)
       (core-schema/add-default schema)))

(comment
  #?(:clj (->> (build)
               (map (fn [[k v]] (format "  * `%s` %s\n" k (::sim-engine/doc v))))
               sort
               (apply str)))
  ;
)
