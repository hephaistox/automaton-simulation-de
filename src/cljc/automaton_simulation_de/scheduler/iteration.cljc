(ns automaton-simulation-de.scheduler.iteration
  "A scheduler iteration is holding the simulation state, past and future events. A scheduler iteration is identified with an id property and is related to exactly one date.  The scheduler is ordering the future events based on the event ordering, so the first one could be executed."
  (:require
   [automaton-core.log :as core-log]
   [automaton-simulation-de.event :as sim-de-event]))

(def scheduler-iteration-schema
  "Scheduler iteration is a map containing:
   * `id` - A counter of scheduler iteration. By convention, the counter is starting at 0. It is in a one to one relationship with a scheduler iteration (so it acts like an id of that scheduler iteration in the context of a scheduler play) in the context scheduler.
   * `state` - map that will be modified by event
   * `past-events` - collection of a past event, which is event that has already been executed, so its date is in the past. A past event cannot be modified anymore (i.e. it is immutable).
   * `future-events` - collection of a future event, which is an event that has not been executed yet, so its date is in the future. A future event may be updated during the simulation - its date may change, its data may change or it could be cancelled (i.e. it’s a mutable event)"
  [:map
   [:id :int]
   [:state :map]
   [:past-events [:sequential sim-de-event/event-schema]]
   [:future-events [:sequential sim-de-event/event-schema]]])

(defn execute
  "Execution of scheduler iteration.

  A scheduler iteration should not access past or future events to have some information on the state the simulation

  Params:
  * `event-registry-kvs` - map complying to `event-registry-kvs-schema`
  * `event-ordering-fn` - comparator function that accepts two events and should return boolean
  * `scheduler-iteration` - map complying to `scheduler-iteration-schema`"
  [event-registry-kvs
   event-ordering-fn
   {:keys [id state past-events future-events]}]
  (let [sorted-future-events (sort event-ordering-fn future-events)
        current-event (first sorted-future-events)
        new-future-events (rest sorted-future-events)
        event-execution
        (or (get event-registry-kvs (:type current-event))
            (do (core-log/error-format "Event of type %s is unknown" type)
                (constantly {:state state
                             :future-events new-future-events})))
        new-past-events (vec (cons current-event past-events))]
    (merge {:id (inc id)
            :past-events new-past-events}
           (event-execution state new-future-events current-event))))
