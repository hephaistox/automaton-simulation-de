(ns automaton-simulation-de.middleware.request
  "Build a request for an handler.

  Contains a stop, the scheduler snapshot and the current event.

  * [See entity](docs/archi/request_entity.png)"
  (:require
   [automaton-simulation-de.scheduler.event           :as sim-de-event]
   [automaton-simulation-de.scheduler.event-execution :as
                                                      sim-de-event-execution]
   [automaton-simulation-de.scheduler.snapshot        :as sim-de-snapshot]))

(defn schema
  []
  [:map {:closed false}
   [::stop [:sequential :map]]
   [::snapshot (sim-de-snapshot/schema)]
   [::event-execution [:maybe (sim-de-event-execution/schema)]]
   [::sorting
    [:function
     [:=>
      [:cat [:vector (sim-de-event/schema)]]
      [:vector (sim-de-event/schema)]]]]])

(defn build
  "Builds a request for the middleware, with iteration dependant data.

  Returns a map, with the data necessary for the request.

  Params:
  * `current-event`
  * `event-execution`
  * `scheduler-snapshot`
  * `sorting`
  * `stop`"
  [current-event event-execution snapshot sorting stop]
  {::stop stop
   ::snapshot snapshot
   ::event-execution event-execution
   ::current-event current-event
   ::sorting sorting})

(defn prepare
  "Creates a request, based on event execution, snapshot and sorting

  Returns the request gathering the snapshot, the sorting, stopping reasons.

  Predefined stopping reasons are when no future events exist.

  Params:
  * `event-execution` event execution, fn that takes a snapshot and return a event-return
  * `snapshot` the snapshot before the iteration
  * `sorting` function that take the future events and returns sorted future events"
  [current-event event-execution snapshot sorting]
  (let [{:keys [::sim-de-snapshot/future-events]} snapshot]
    (build current-event
           event-execution
           snapshot
           sorting
           (cond-> []
             (empty? future-events) (conj {:cause ::no-future-events})))))

(defn add-stop
  "Add map `m` among stop causes
  Params:
  * `request`
  * `m` map"
  [request m]
  (update request ::stop conj m))
