(ns automaton-simulation-de.events.logger
  "This event log the event at the given date"
  (:require [automaton-simulation-de.events :as automaton-events]
            [automaton-core.log :as log]))

(defrecord LoggerEvent [date* message-fn]
  automaton-events/DiscreteEvent
    (date [_] date*)
    (process [_ state] (do (log/trace (message-fn @state)) @state))
    (after [_ event] (>= date* (:date* event))))

(defn make-logger-event
  "Creates a logger event
  Params:
  * `date`
  * `message-fn` is a function taking the state value as a parameter"
  [date message-fn]
  (->LoggerEvent date message-fn))
