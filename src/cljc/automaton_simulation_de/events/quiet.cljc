(ns automaton-simulation-de.events.quiet
  "A noop event"
  (:require [automaton-simulation-de.events :as automaton-events]))

(defrecord QuietEvent [date* data]
  automaton-events/DiscreteEvent
    (date [_] date*)
    (process [_ state] @state)
    (after [_ event] (>= date* (:date* event))))

(defn make-quiet-event
  "Creates the event
  Params:
  * `date`
  * `data` some data you can store here"
  [date data]
  (->QuietEvent date data))
