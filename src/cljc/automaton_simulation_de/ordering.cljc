(ns automaton-simulation-de.ordering
  "Event ordering is a part of the event registry meant to sort the future events.

  It is especially important to manage simultaneous events. These events does not exists in the real life, but the.

  * [See entity](docs/archi/ordering_entity.png)

  ## Date ordering
  Date ordering is sorting events by date
  It could be done with `compare-field` on the date field of the event (typically `:sim-de-event/date`)

  ## Simultaneous event ordering
  A simultaneous event ordering apply user defined decision on which event should be executed first
  It could leverage `compare-types` and `compare-field` or what user defined functions

  ## Simultaneous events
  Two or more events are simultaneous if they happen at the same date in the scheduler."
  (:require
   [automaton-simulation-de.scheduler.event :as sim-de-event]))

(defn compare-field
  "Returns a function to compare `e1` and `e2` based on values of field `field`

  nil values are accepted

  Params:
  * `e1` is an event
  * `e2` is an event"
  [field]
  (fn [e1 e2]
    (let [d1 (field e1)
          d2 (field e2)]
      (cond
        (nil? d1) 666
        (nil? d2) -666
        :else (compare d1 d2)))))

(defn compare-types
  "Compares two events with their types, based on their ordering in `evt-type-priorities`

  Params:
  * `evt-type-priorities` list of event priorities, ordered with higher priority first in the list
  * `e1` first event
  * `e2` second event
  both events as defined in `automaton-simulation-de.event`

  Returns true if `e1` is before `e2`"
  [evt-type-priorities]
  (fn [e1 e2]
    (let [te1 (::sim-de-event/type e1)
          te2 (::sim-de-event/type e2)]
      (cond
        (nil? te1) 666
        (nil? te2) -666
        (not= te1 te2) (- (.indexOf evt-type-priorities te1)
                          (.indexOf evt-type-priorities te2))
        :else 0))))

(defn- orders
  "Orders events by date, if events have the same date (`date-ordering` returns nil), than they are sorted with `same-date-ordering`.
  Applies caller defined decision on which event should be executed first

  Params:
  * `event-sortings` matching `schema`
  * `e1` event
  * `e2` event
  both events as defined in `automaton-simulation-de.event`"
  [event-orderings e1 e2]
  (loop [event-orderings event-orderings]
    (let [event-ordering (first event-orderings)
          res (if (some? event-ordering) (event-ordering e1 e2) 0)]
      (cond
        (nil? event-ordering) 0
        (zero? res) (recur (rest event-orderings))
        :else res))))

(defn sorter
  "Returns a function with two events as parameters and returning the comparison of them, according to event-orderings

  Params:
  * `events-orderings`"
  [event-orderings]
  (fn [events] (sort (fn [e1 e2] (orders event-orderings e1 e2)) events)))
