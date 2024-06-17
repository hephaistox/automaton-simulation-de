(ns automaton-simulation-de.simulation-engine.request
  "Build a request for an handler.

  Contains:

  * `current-event`
  * `event-execution`
  * `snapshot`
  * `sorting`
  * `stopping-causes`, the scheduler snapshot and the current event.
  * ![entities](archi/request_entity.png)"
  (:require
   [automaton-simulation-de.simulation-engine                 :as-alias
                                                              sim-engine]
   [automaton-simulation-de.simulation-engine.event           :as sim-de-event]
   [automaton-simulation-de.simulation-engine.event-execution
    :as sim-de-event-execution]
   [automaton-simulation-de.simulation-engine.snapshot        :as
                                                              sim-de-snapshot]))

(def schema
  [:map {:closed false}
   [::sim-engine/current-event sim-de-event/schema]
   [::sim-engine/event-execution [:maybe sim-de-event-execution/schema]]
   [::sim-engine/snapshot sim-de-snapshot/schema]
   [::sim-engine/sorting
    [:function
     [:=> [:cat [:vector sim-de-event/schema]] [:vector sim-de-event/schema]]]]
   [::sim-engine/stopping-causes [:sequential :map]]])

(defn add-stopping-cause
  "Adds to the `request` the map `m` among `stopping-causes`."
  [request m]
  (if (nil? m) request (update request ::sim-engine/stopping-causes conj m)))
