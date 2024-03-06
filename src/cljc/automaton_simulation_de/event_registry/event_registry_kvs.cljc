(ns automaton-simulation-de.event-registry.event-registry-kvs
  "Event registry key value associates an event type to an event execution"
  (:require
   [automaton-simulation-de.event :as sim-de-event]))

(def event-registry-kvs-schema
  "Map of event-registry-kv pairs
  Where k is event type
  and v is event execution.
  The event type is used to define what event execution will happen.
  An event execution is updating the state and may trigger some new events to be added in the future events. The result of the event execution could be dependent on the state or random variable."
  [:map-of
   :keyword
   [:=>
    [:cat
     :map
     [:sequential sim-de-event/event-schema]
     sim-de-event/event-schema]
    [:map
     [:state :map]
     [:future-events [:sequential sim-de-event/event-schema]]]]])
