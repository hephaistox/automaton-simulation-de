(ns automaton-simulation-de.scheduler.event-return
  "What is returned by an event"
  (:refer-clojure :exclude [nth])
  (:require
   [automaton-simulation-de.scheduler.event :as sim-de-event]))

(defn schema
  []
  [:map {:closed true}
   [::state :any]
   [::future-events [:sequential (sim-de-event/schema)]]])

(defn build
  [state future-events]
  {::state state
   ::future-events future-events})

(defn add-event
  "Adds `event` to the event-return, so it will be executed as a future event.
  `date` is an optional parameter that impose to the event the date it should be executed."
  ([event-return event]
   (cond-> event-return
     (map? event) (update ::future-events conj event)))
  ([event-return event date]
   (cond-> event-return
     (map? event)
     (update ::future-events conj (assoc event ::sim-de-event/date date)))))

(defn add-events
  "Add events to the future-events."
  [event-return event]
  (update event-return ::future-events concat event))

(defn nth
  "Uniformly choose one event among many."
  ([event-return events-to-choose]
   (add-event event-return (clojure.core/rand-nth events-to-choose)))
  ([event-return events-to-choose date]
   (add-event event-return (clojure.core/rand-nth events-to-choose) date)))

(defn if-return
  [event-return condition then-fn else-fn]
  (if condition (then-fn event-return) (else-fn event-return)))
