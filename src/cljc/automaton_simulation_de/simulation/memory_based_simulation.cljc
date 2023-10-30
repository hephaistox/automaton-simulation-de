(ns automaton-simulation-de.simulation.memory-based-simulation
  "Simulation all based in memory"
  (:require [automaton-simulation-de.simulation :as simulation]))

(defrecord MemorySimulation [state* scheduler]
  simulation/Simulation
    (state [_] state*))

(defn make-memory-simulation
  "Initialize a simulation
  Params:
  * `init-state` is a map with the initial state
  * `scheduler` is an implementation of `automaton-simulation-de.scheduler/DiscreteEventScheduler`"
  [init-state scheduler]
  (->MemorySimulation (atom init-state) scheduler))
