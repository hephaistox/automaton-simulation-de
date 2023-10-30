(ns automaton-simulation-de.simulation)

(defprotocol Simulation
  (state [_]
    "Returns the current of the simulation"))
