(ns automaton-simulation-de.scheduler.snapshot
  "A scheduler snapshot is a consistent set of data for the scheduler.
It is containing the simulation state, past events and  future events and iteration number scheduler snapshot is identified with an id property and is related to exactly one date.  Past and Future events are sorted collection of events.

Remarks:
* An entity using the scheduler snapshot should not access past or future events to have some information on the state the simulation.

  * [See entity](docs/archi/snapshot_entity.png)"
  (:require
   [automaton-simulation-de.scheduler.event        :as sim-de-event]
   [automaton-simulation-de.scheduler.event-return :as sim-de-event-return]))

(defn schema
  "Scheduler snapshot is a map containing:
   * `id` - Unique identificator of an scheduler snapshot.  It is in a one to one relationship with a scheduler snapshot (so it acts like an id of that scheduler snapshot in the context of a scheduler play) in the context scheduler.
   * `iteration` - A counter of scheduler iteration. By convention, the counter is starting at 0.
   * `date` - Defines the date where that scheduler-snapshot happens
   * `state` - State modified by the event
   * `past-events` - Collection of a past event. A past event is event that has already been executed, so its date is in the past. A past event cannot be modified anymore (i.e. it is immutable).
   * `future-events` - Collection of a future event. A future event is an event that has not been executed yet, so its date is in the future. A future event may be updated during the simulation - its date may change, its data may change or it could be cancelled (i.e. it’s a mutable event)"
  []
  [:map {:closed true}
   [::id :int]
   [::iteration :int]
   [::date :any]
   [::state :any]
   [::past-events [:sequential (sim-de-event/schema)]]
   [::future-events [:sequential (sim-de-event/schema)]]])

(defn next-snapshot
  "Creates the next snapshot based on the previous one and decision of what is the event and new-past-events and new-future-events

  Params:
  * `previous-snapshot`
  Returns a snapshot."
  [previous-snapshot]
  (let [{:keys [::id ::iteration ::state ::past-events ::future-events]}
        previous-snapshot
        [event & new-future-events] future-events
        {:keys [::sim-de-event/date]} event
        new-past-events (if (nil? event) past-events (conj past-events event))]
    {::id ((fnil inc 0) id)
     ::iteration ((fnil inc 0) iteration)
     ::date (if (nil? date) (::date previous-snapshot) date)
     ::state state
     ::past-events (if (nil? new-past-events) [] new-past-events)
     ::future-events (if (nil? new-future-events) [] new-future-events)}))

(defn build
  "Creates a snapshot"
  [id iteration date state past-events future-events]
  {::id id
   ::iteration iteration
   ::date date
   ::state state
   ::past-events (if (nil? past-events) [] past-events)
   ::future-events (if (nil? future-events) [] future-events)})

(defn inconsistency?
  "Check snapshot consistency

  Returns sequence of future events which are before current date
  or past events which are after current date
  Returns false if consistent"
  [{:keys [::date]
    :as snapshot}]
  (if (nil? date)
    ::nil-date
    (let [res (-> (select-keys snapshot [::future-events ::past-events])
                  (update ::future-events
                          (partial filterv #(> date (::sim-de-event/date %))))
                  (update ::past-events
                          (partial filterv #(< date (::sim-de-event/date %)))))]
      (if (= res
             {::future-events []
              ::past-events []})
        false
        res))))

(defn update-snapshot-with-event-return
  "Updates the `snapshot` with the `event-return` `state` and `future-events` data.

  * Design decision: `state` and `future-events` are replaced with values returned by the event execution
     * Rationale:
       * State structure need not to be known from here
       * Merging the old value and a chunk is not efficient
     * Consequence
       * The events should update the value of the state
       * The events should not update the difference only
     * Limits
       * Not known

  Params:
  * `event-return` return of the event to execute
  * `sorting` function taking a sequence of `future-events` and returns the sorted sequence
  * `snapshot` "
  [event-return sorting snapshot]
  (let [{:keys [::sim-de-event-return/state
                ::sim-de-event-return/future-events]}
        event-return]
    (-> snapshot
        (assoc ::future-events
               (or (if (fn? sorting) (sorting future-events) future-events) [])
               ::state state))))
