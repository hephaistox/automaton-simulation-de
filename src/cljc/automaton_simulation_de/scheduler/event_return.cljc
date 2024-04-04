(ns automaton-simulation-de.scheduler.event-return
  "What is returned by an event"
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
