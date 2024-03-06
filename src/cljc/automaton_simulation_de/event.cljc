(ns automaton-simulation-de.event
  "An event is an individual happening, taking place at a given moment. Each event is indivisible and instantaneous. The event execution is defined by its event type.

  Discreteness is a property of an event stating that the changes in the state could be counted (i.e. changes are discrete in the mathematical definition)

  Each event defines following outputs:
  * a simulation state update regarding the event that happened.
  * a list of events that should be inserted in the future events
  * some events that are postponed.
  * some events that are cancelled.")

(def event-schema
  "Event happens at a `:date` and has a `:type`"
  [:map [:type :keyword] [:date :any]])

(defn postpone-events
  "For all events in `events`, its date is postponed to `d` if `(event-filter-fn event)` returns true
  Params:
  * `events` list of event to scan
  * `event-filter-fn` function apply to each event (event-filter-fn event)
  * `d` date to postpone to"
  [events event-filter-fn d]
  (mapv (fn [event] (if (event-filter-fn event) (assoc event :date d) event))
        events))
