(ns automaton-simulation-de.demo.queuing.events
  "Events to manage queueing

  Arrival is a poisson distribution"
  (:require [automaton-simulation-de.events :as simulation-events]))



(defrecord Arrival [date*]
  simulation-events/DiscreteEvent
    (date [_] date*)
    (process [_ state] (update state :input-stock)))

(defrecord Departure [date*]
  simulation-events/DiscreteEvent
    (date [_] date*)
    (process [_ state]
      "Process that event\nParams:\n* `state` state. Returns the updated state value"))

(defrecord ProcessingStart [date*]
  simulation-events/DiscreteEvent
    (date [_] date*)
    (process [_ state]
      "Process that event\nParams:\n* `state` state. Returns the updated state value"))

(defrecord ProcessingEnd [date*]
  simulation-events/DiscreteEvent
    (date [_] date*)
    (process [_ state]
      "Process that event\nParams:\n* `state` state. Returns the updated state value"))
