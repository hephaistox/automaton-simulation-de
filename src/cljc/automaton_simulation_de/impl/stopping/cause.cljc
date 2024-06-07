(ns automaton-simulation-de.impl.stopping.cause
  "A reason why the `scheduler` stops. A `stopping-cause` contains:

  * `context` data describing the context of the `stopping-criteria`, note its schema is defined freely by each `stopping-definition`.
  * `current-event` the event that is about to be executed now. It could be null, if future-event is nil and no first event found.
  * `stopping-criteria` telling the intent of the user to stop.

  ![Entities](archi/simulation_engine/stopping_cause.png)"
  (:require
   [automaton-simulation-de.impl.stopping.criteria :as sim-de-criteria]
   [automaton-simulation-de.scheduler.event        :as sim-de-event]))

(def schema
  [:map {:closed true}
   [:context {:optional true}
    :map]
   [:current-event [:maybe sim-de-event/schema]]
   [:stopping-criteria sim-de-criteria/schema]])
