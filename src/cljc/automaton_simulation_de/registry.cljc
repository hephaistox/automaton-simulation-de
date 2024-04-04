(ns automaton-simulation-de.registry
  "Event registry contains Event registry kv.

  Event registry kv associates one event type to event execution.

  * [See entity](docs/archi/registry_entity.png)"
  (:refer-clojure :exclude [get])
  (:require
   [automaton-simulation-de.middleware.response       :as sim-de-response]
   [automaton-simulation-de.scheduler.event           :as sim-de-event]
   [automaton-simulation-de.scheduler.event-execution :as
                                                      sim-de-event-execution]))

(defn schema
  "Map associating `event-type` to `event-execution`.
  An event execution is updating the state and may trigger some new events to be added in the future events. The result of the event execution could be dependent on the state or random variable."
  []
  [:map-of :keyword (sim-de-event-execution/schema)])

(defn get
  "Get the execution based on the type.

  Params:
  * `event-registry` is a map of event type to event execution
  * `event` event which code will be executed

  Returns the execution function
  * with parameters:
     * `state`
     * `future-events`
     * `event`
  * Returns what changes in the `scheduler-iteration` (i.e. a map with `state` and `future-events`)"
  [event-registry event]
  (clojure.core/get event-registry (::sim-de-event/type event) nil))

(defn add-registry-stop
  "Adds a stop reason at `::execution` if the registry entry is not found
  Params:
  * `response`
  * `event`"
  [response event]
  (sim-de-response/add-stop response
                            {:cause ::execution-not-found
                             :not-found-type (::sim-de-event/type event)}))
