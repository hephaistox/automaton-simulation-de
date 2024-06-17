(ns automaton-simulation-de.simulation-engine.impl.event-registry
  "The `event-registry` contains `event-execution`.

  It associates one event type (a keyword) to its `event-execution`.

  * [See entity](docs/archi/registry_entity.png)"
  (:require
   [automaton-simulation-de.simulation-engine.event-execution
    :as sim-de-event-execution]))

(def schema
  "Map associating `event-type` to `event-execution`.
  An event execution is updating the state and may trigger some new events to be added in the future events. The result of the event execution could be dependent on the state or random variable."
  [:map-of :keyword sim-de-event-execution/schema])
