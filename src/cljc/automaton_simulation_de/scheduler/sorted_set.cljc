(ns automaton-simulation-de.scheduler.sorted-set
  "Implementaton of a scheduler based on a sorted set of events.

  For future events, the data structure is initialiazed with a sorted set based on the date, so all further operations keep that property
  For past events, they need not to change, so it's a vector which is efficient to add elements to the tail, so past events will be ordered in the order of their execution"
  (:require [automaton-simulation-de.scheduler :as simulation-scheduler]
            [automaton-simulation-de.events :as simulation-events]))

(defrecord DiscreteEventScheduler [past-events future-events]
  simulation-scheduler/DiscreteEventScheduler
    (add-event [_ event]
      (->DiscreteEventScheduler past-events (conj future-events event)))
    (next-event [_] (first future-events))
    (last-event [_] (peek past-events))
    (pop-event [this]
      (if-let [next-event (simulation-scheduler/next-event this)]
        (do (println "next-event " next-event)
            (->DiscreteEventScheduler (conj past-events next-event)
                                      (disj future-events next-event)))
        this))
    (future-events [_] future-events)
    (ended? [_] (empty? future-events))
    (past-events [_] past-events)
    (all-events [_]
      (-> (concat past-events future-events)
          vec)))

(defn- sort-date
  "This"
  [event-comparator]
  (fn [a b]
    (let [date-a (simulation-events/date a)
          date-b (simulation-events/date b)]
      (if (= date-a date-b) (event-comparator a b) (< date-a date-b)))))

(defn make-discrete-event-scheduler
  "Creates an event scheduler"
  [event-comparator]
  (let [future-events (sorted-set-by (sort-date event-comparator))]
    (->DiscreteEventScheduler [] future-events)))

;;TODO Add a graph execution logger
