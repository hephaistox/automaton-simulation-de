(ns automaton-simulation-de.simulation-engine.event-return
  "What is returned by an event."
  (:refer-clojure :exclude [nth])
  (:require
   [automaton-simulation-de.simulation-engine          :as-alias sim-engine]
   [automaton-simulation-de.simulation-engine.event    :as sim-de-event]
   [automaton-simulation-de.simulation-engine.snapshot :as sim-de-snapshot]))

(def schema
  [:map {:closed true}
   [::sim-engine/state :any]
   [::sim-engine/future-events [:sequential sim-de-event/schema]]])

(defn add-event
  "Adds `event` to the event-return, so it will be executed as a future event.
  `date` is an optional parameter that impose to the event the date it should be executed."
  ([event-return event]
   (cond-> event-return
     (map? event) (update ::sim-engine/future-events conj event)))
  ([event-return event date]
   (cond-> event-return
     (map? event) (update ::sim-engine/future-events
                          conj
                          (assoc event ::sim-engine/date date)))))

(defn add-events
  "Add events to the future-events."
  [event-return event]
  (update event-return ::sim-engine/future-events concat event))

(defn nth
  "Uniformly choose one event among many."
  ([event-return events-to-choose]
   (add-event event-return (clojure.core/rand-nth events-to-choose)))
  ([event-return events-to-choose date]
   (add-event event-return (clojure.core/rand-nth events-to-choose) date)))

(defn if-return
  [event-return condition then-fn else-fn]
  (if condition (then-fn event-return) (else-fn event-return)))

(defn update-snapshot
  [snapshot {::sim-engine/keys [future-events state]}]
  (sim-de-snapshot/update snapshot future-events state))
