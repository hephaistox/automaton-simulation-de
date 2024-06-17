(ns automaton-simulation-de.simulation-engine.snapshot
  "A scheduler snapshot is a consistent set of data for the scheduler.
It is containing the simulation state, past events and future events and iteration number scheduler snapshot is identified with an id property and is related to exactly one date. Past and Future events are sorted collection of events.

Remarks:
* An entity using the scheduler snapshot should not access past or future events to have some information on the state the simulation.

  * [See entity](docs/archi/snapshot_entity.png)"
  (:refer-clojure :exclude [update])
  (:require
   [automaton-simulation-de.simulation-engine       :as-alias sim-engine]
   [automaton-simulation-de.simulation-engine.event :as sim-de-event]))

(def schema
  "Scheduler snapshot is a map containing:
   * `id` - Unique identificator of an scheduler snapshot.  It is in a one to one relationship with a scheduler snapshot (so it acts like an id of that scheduler snapshot in the context of a scheduler play) in the context scheduler.
   * `iteration` - A counter of scheduler iteration. By convention, the counter is starting at 1.
   * `date` - Defines the date where that scheduler-snapshot happens
   * `state` - State modified by the event
   * `past-events` - Collection of a past event. A past event is event that has already been executed, so its date is in the past. A past event cannot be modified anymore (i.e. it is immutable).
   * `future-events` - Collection of a future event. A future event is an event that has not been executed yet, so its date is in the future. A future event may be updated during the simulation - its date may change, its data may change or it could be cancelled (i.e. it’s a mutable event)"
  [:map {:closed true}
   [::sim-engine/id :int]
   [::sim-engine/iteration :int]
   [::sim-engine/date :any]
   [::sim-engine/state :any]
   [::sim-engine/past-events [:sequential sim-de-event/schema]]
   [::sim-engine/future-events [:sequential sim-de-event/schema]]])

(defn consume-first-event
  "Returns a `snapshot` where the first `future-event` is moved to the `past-events` and `date` is updated to this `date`."
  [{::sim-engine/keys [id iteration state past-events future-events date]
    :as _snapshot}]
  (let [[event & new-future-events] future-events
        {event-date ::sim-engine/date} event
        new-past-events (if (nil? event) past-events (conj past-events event))]
    {::sim-engine/id ((fnil inc 0) id)
     ::sim-engine/iteration iteration
     ::sim-engine/date (if (nil? event-date) date event-date)
     ::sim-engine/state state
     ::sim-engine/past-events (if (nil? new-past-events) [] new-past-events)
     ::sim-engine/future-events
     (if (nil? new-future-events) [] new-future-events)}))

(defn next-iteration
  "Update the `snapshot` to the next iteration."
  [snapshot]
  (clojure.core/update snapshot ::sim-engine/iteration (fnil inc 0)))

(defn initial
  "Creates an initial snapshsot"
  [starting-evt-type date]
  {::sim-engine/id 1
   ::sim-engine/iteration 1
   ::sim-engine/date date
   ::sim-engine/state {}
   ::sim-engine/past-events []
   ::sim-engine/future-events [{::sim-engine/type starting-evt-type
                                ::sim-engine/date date}]})

(defn inconsistency?
  "Check snapshot consistency

  Returns:
  * `::sim-engine/nil-date` if there is no date set in the snapshot.
  * sequence of future events which are before current date
  * past events which are after current date
  Returns false if consistent"
  [{::sim-engine/keys [date]
    :as snapshot}]
  (if (nil? date)
    ::sim-engine/nil-date
    (let [res (-> (select-keys snapshot
                               [::sim-engine/future-events
                                ::sim-engine/past-events])
                  (clojure.core/update ::sim-engine/future-events
                                       (partial filterv
                                                #(let [d (::sim-engine/date %)]
                                                   (or (nil? d) (> date d)))))
                  (clojure.core/update
                   ::sim-engine/past-events
                   (partial filterv
                            #(let [d (::sim-engine/date %)]
                               (or (nil? d) (< date (::sim-engine/date %)))))))]
      (if (= res
             {::sim-engine/future-events []
              ::sim-engine/past-events []})
        false
        {:snapshot-date date
         :mismatching-events res}))))

(defn update
  "Helper to update `future-events` and `state`.
  Please note both will be replaced and not updated."
  [snapshot future-events state]
  (assoc snapshot
         ::sim-engine/future-events future-events
         ::sim-engine/state state))

(defn sort-future-events
  "Sort `future-events` in `snapshot` thanks the `sorting` sorter."
  [snapshot sorting]
  (clojure.core/update
   snapshot
   ::sim-engine/future-events
   (fn [future-events]
     (or (if (fn? sorting) (sorting future-events) future-events) []))))
