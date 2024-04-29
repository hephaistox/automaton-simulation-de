(ns automaton-simulation-de.core
  "Simulation is a technique that mimics a real system - and simplifies it, to learn something useful about it.
Discrete event simulation is modeling a real system with discrete events

* Contains a user-specific domain, constraints, a customer-specific state and events modeling and an option to render visually the effects.
* Customer simulation can use directly `DE Simulation` or a library that eases the modeling: `rc modeling`, `industry modeling`, … "
  (:require
   [automaton-simulation-de.impl.model                 :as sim-de-model]
   [automaton-simulation-de.impl.scheduler             :as sim-de-scheduler]
   [automaton-simulation-de.middleware.state-rendering :as
                                                       sim-de-state-rendering]
   [automaton-simulation-de.scheduler.snapshot         :as sim-de-snapshot]))

(defn build-model
  "Build the simulation model to gather information required to run the simulation."
  [registry middlewares ordering initial-snapshot max-iteration]
  (sim-de-model/build registry
                      middlewares
                      ordering
                      initial-snapshot
                      max-iteration))

(defn scheduler
  "Scheduler is running the simulation described in the `model`.
  Returns the simulation `snapshot` of the last event."
  [model]
  (sim-de-scheduler/scheduler model))

(defn state-printing
  "Apply the `rendering-fn` at each iteration of the scheduler, so each `state` change."
  [rendering-fn handler]
  (sim-de-state-rendering/state-printing rendering-fn handler))

(defn initial
  "Creates an initial snapshsot"
  [starting-evt-type date]
  (sim-de-snapshot/initial starting-evt-type date))
