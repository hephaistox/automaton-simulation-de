(ns automaton-simulation-de.request
  "Build a request for an handler.

  Contains:

  * `event-execution`
  * `snapshot`
  * `sorting`
  * `stopping-causes`, the scheduler snapshot and the current event.
  * ![entities](archi/request_entity.png)"
  (:require
   [automaton-simulation-de.scheduler.event           :as sim-de-event]
   [automaton-simulation-de.scheduler.event-execution :as
                                                      sim-de-event-execution]
   [automaton-simulation-de.scheduler.snapshot        :as sim-de-snapshot]))

(def schema
  [:map {:closed false}
   [::event-execution [:maybe sim-de-event-execution/schema]]
   [::snapshot sim-de-snapshot/schema]
   [::current-event sim-de-event/schema]
   [::sorting
    [:function
     [:=> [:cat [:vector sim-de-event/schema]] [:vector sim-de-event/schema]]]]
   [::stopping-causes [:sequential :map]]])

(defn add-stopping-cause
  "Adds to the `request` the map `m` among `stopping-causes`."
  [request m]
  (if (nil? m) request (update request ::stopping-causes conj m)))

(defn build
  "Builds a request for the middleware, with iteration dependant data.
  Note that `current-event` is not leveraged by the scheduler, the real `current-event` is the first in the `future-list` of the `snapshot`. That said `current-event` is useful for middlewares.

  Returns a map, with the data necessary for the request."
  [current-event event-execution snapshot sorting]
  {::stopping-causes []
   ::snapshot snapshot
   ::event-execution event-execution
   ::current-event current-event
   ::sorting sorting})
