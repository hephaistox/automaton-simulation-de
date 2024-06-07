(ns automaton-simulation-de.impl.stopping-definition.iteration-nth
  "`stopping-definition` to stop at a given iteration."
  (:require
   [automaton-simulation-de.scheduler.snapshot :as sim-de-snapshot]))

(defn stop-nth
  "Is the `snapshot`'s `iteration` greater than or equal to `n`, the parameter in `params`."
  [snapshot
   {:keys [n]
    :as _params}]
  (let [snapshot-iteration (get snapshot ::sim-de-snapshot/iteration 0)]
    {:stop? (or (nil? n) (>= snapshot-iteration n))
     :context {:iteration snapshot-iteration
               :n n}}))

(defn stopping-definition
  []
  {:doc "Stops when the iteration `n` is reached."
   :id :iteration-nth
   :next-possible? true
   :stopping-evaluation stop-nth})
