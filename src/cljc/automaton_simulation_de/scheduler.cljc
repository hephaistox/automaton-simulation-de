(ns automaton-simulation-de.scheduler
  "Scheduler

  A scheduler must have the following properties:
  * Causality: no future event can modify the state in the past
  * next-event should return the lowest date in the future
  * in case of equality in event's date, a [total ordering](https://en.wikipedia.org/wiki/Total_order) based on the only information of the event
  * no event could be added with a date in the past, it could be the same date that the one in the past, but could not be in the future ")

(defprotocol DiscreteEventScheduler
  (add-event [this event]
    "Add an event in the scheduler.\nParams:\n* `event` must implement the `automaton-simulation-de.events/DiscreteEvent`")
  (last-event [this]
    "Returns the last executed event in the scheduler")
  (next-event [this]
    "Returns the next event in the scheduler")
  (ended? [this]
    "Returns true if the scheduler has no future event any more")
  (pop-event [this]
    "Pop the next event in the scheduler")
  (future-events [this]
    "List of events in the future")
  (past-events [this]
    "List of past events")
  (all-events [this]
    "List of all events"))
