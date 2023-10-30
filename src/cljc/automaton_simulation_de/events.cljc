(ns automaton-simulation-de.events "Events of a discrete event simulation")

(defprotocol DiscreteEvent
  (date [this]
    "Current date of that event")
  (process [this state]
    "Process that event\nParams:\n* `state` state. Returns the updated state value"))
