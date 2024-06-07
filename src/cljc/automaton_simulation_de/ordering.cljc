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

(def schema [:function [:=> [:cat :any :any] :boolean]])

(defn compare-field
  "Returns a function to compare `e1` and `e2` based on values of field `field`. `nil` values are accepted."
  [field]
  (fn [e1 e2]
    (let [d1 (field e1)
          d2 (field e2)]
      (cond
        (nil? d1) 666
        (nil? d2) -666
        :else (compare d1 d2)))))

(defn compare-types
  "Compares two events `e1` and `e2` with their types, based on their ordering in `evt-type-priorities`, list of event priorities, ordered with higher priority first in the list.
  Returns the difference of their position in the `evt-type-priorities`, which is `0` in case of equality, negative if `e1` is before `e2`."
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
  "Orders events by date.
  Applies caller defined decision on which event should be executed first."
  [orderings e1 e2]
  (loop [orderings orderings]
    (let [ordering (first orderings)
          res (if (some? ordering) (ordering e1 e2) 0)]
      (cond
        (nil? ordering) 0
        (zero? res) (recur (rest orderings))
        :else res))))

(defn sorter
  "Returns a function with two events as parameters and returning the comparison of them, according to event-orderings."
  [orderings]
  (fn [events] (sort (fn [e1 e2] (orders orderings e1 e2)) events)))

(defn data-to-fn
  [[kind data :as _ordering-data]]
  (case kind
    :field (compare-field data)
    :type (compare-types data)))
