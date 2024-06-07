(ns automaton-simulation-de.impl.built-in-sd.registry
  "built-in `stopping-definition` can create `stopping-cause` but they are not accessible for modellers, they are hard coded."
  (:require
   [automaton-core.adapters.schema                                  :as
                                                                    core-schema]
   [automaton-core.utils.map                                        :as
                                                                    utils-map]
   [automaton-simulation-de.impl.built-in-sd.causality-broken
    :as sim-de-causality-broken]
   [automaton-simulation-de.impl.built-in-sd.execution-not-found
    :as sim-de-execution-not-found]
   [automaton-simulation-de.impl.built-in-sd.failed-event-execution
    :as sim-failed-event-execution]
   [automaton-simulation-de.impl.built-in-sd.no-future-events
    :as sim-de-no-future-events]
   [automaton-simulation-de.impl.stopping.definition
    :as sim-de-sc-definition]))

(defn schema
  []
  [:map-of sim-de-sc-definition/id-schema sim-de-sc-definition/schema])

(defn build
  " The built-in stopping definition are:

  * `:causality-broken` Stops when no future events exists anymore.
  * `:execution-not-found` Stops when the execution of an event is not found in the registry.
  * `:failed-event-execution` Stops when an execution has raised an exception.
  * `:no-future-events` Stops when no future events exists anymore. "
  []
  (->> (utils-map/maps-to-key [sim-de-causality-broken/stopping-definition
                               sim-de-execution-not-found/stopping-definition
                               sim-failed-event-execution/stopping-definition
                               sim-de-no-future-events/stopping-definition]
                              :id)
       (core-schema/add-default (schema))))

(comment
  #?(:clj (->> (build)
               (map (fn [[k v]] (format "  * `%s` %s\n" k (:doc v))))
               sort
               (apply str)))
  ;
)
