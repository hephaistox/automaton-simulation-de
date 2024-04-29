(ns automaton-simulation-de.scheduler.event
  "An event is an individual happening, taking place at a given moment. Each event is indivisible and instantaneous. The event execution is defined by its event type.

  Discreteness is a property of an event stating that the changes in the state could be counted (i.e. changes are discrete in the mathematical definition).

  Each event defines following outputs:
   * a simulation state update regarding the event that happened.
   * a list of events that should be inserted in the future events.
   * some events that are postponed.
   * some events that are cancelled.

  [See the entity](docs/archi/event_entity.png)

  An event type: The event type is used to define what event execution will happen.

  Event structure is a map with at least two keys:
  * `type` a the event type as found in `evt-type-priority`.
  * `date` as a date.")

(defn schema
  "An event is a tuple which first value is a keyword for the event type, the second is a date."
  []
  [:map {:closed false}
   [::date :any]
   [::type :keyword]])

(defn make-event
  "Event builder."
  [type date]
  {::type type
   ::date date})

(defn make-events
  "Event builder based on `evt-defs`, a collection of pair type and date."
  [& evt-defs]
  (mapv (partial apply make-event) (partition 2 evt-defs)))

(defn postpone-events
  "Scan event in `events` for which `(event-filter-fn event)` is true, its date is postponed to `date`."
  [events event-filter-fn date]
  (mapv (fn [event]
          (if (event-filter-fn event) (assoc event ::date date) event))
        events))
