# Simulation engine

In discrete event simulation, the simulation engine deals with events and their execution.

The main concerns of the simulation engine are to:
* manage the ordering of future orders
* Execute the first event in the future event to update state and future events
* Consume this event so it is moved to past-event,
* Applying stopping criteria to decide if it continues or not,
* Applying execution of middlewares to add behaviors at each iteration, as persistence.


## Use event parameters
When an event is executed, it may need some data to do its execution.
Previous examples were such, that the name of the event is completely defining its behavior.
Using params allows us to write fewer event types, and more easily compose events together.

```clojure
(defn )
+
```
