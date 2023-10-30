(ns automaton-simulation-de.random "Random number generator")

(defprotocol RandomNumberGenerator
  (next-int [_ max-int]
    "Return the next integer number between [0; max-int [ (i.e. max-int) is excluded")
  (next-float [_ min max]
    "Return a float in [min, max[ (max excluded) its precision is the responsability of the implementation"))
