(ns automaton-simulation-de.simulation-engine.event-execution
  "An event execution is updating the state and may trigger some new events to be added in the future events. The result of the event execution could be dependent on the state or random variable.

  * [See entity](docs/archi/event_execution.png)"
  (:require
   [automaton-simulation-de.simulation-engine.event        :as sim-de-event]
   [automaton-simulation-de.simulation-engine.event-return :as sim-de-event-return]))

(def schema
  "An event is a tuple which first value is a keyword for the event type, the second is a date"
  [:function
   [:=>
    [:cat :any [:sequential sim-de-event/schema] sim-de-event/schema]
    sim-de-event-return/schema]])
