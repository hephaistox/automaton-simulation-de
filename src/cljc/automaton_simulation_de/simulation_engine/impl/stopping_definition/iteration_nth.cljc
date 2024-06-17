(ns
  automaton-simulation-de.simulation-engine.impl.stopping-definition.iteration-nth
  "`stopping-definition` to stop at a given iteration."
  (:require
   [automaton-simulation-de.simulation-engine :as-alias sim-engine]))

(defn stop-nth
  "Is the `snapshot`'s `iteration` greater than or equal to `n`, the parameter in `params`."
  [snapshot
   {::sim-engine/keys [n]
    :as _params}]
  (let [snapshot-iteration (get snapshot ::sim-engine/iteration 0)]
    #:automaton-simulation-de.simulation-engine{:stop?
                                                (or (nil? n)
                                                    (>= snapshot-iteration n))
                                                :context
                                                #:automaton-simulation-de.simulation-engine{:iteration
                                                                                            snapshot-iteration
                                                                                            :n
                                                                                            n}}))

(defn stopping-definition
  []
  #:automaton-simulation-de.simulation-engine{:doc
                                              "Stops when the iteration `n` is reached."
                                              :id ::sim-engine/iteration-nth
                                              :next-possible? true
                                              :stopping-evaluation stop-nth})
