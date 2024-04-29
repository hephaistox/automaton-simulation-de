(ns automaton-simulation-de.middleware.iteration-n
  "Iterates until a given iteration number"
  (:require
   [automaton-simulation-de.middleware.request :as sim-de-request]
   [automaton-simulation-de.scheduler.snapshot :as sim-de-snapshot]))

(defn wrap-iteration-n
  "Stop after the iteration numbered `n`.
  Writes `::stop` in the request under the `::scheduler-middleware-request/stop` keyword."
  [n handler]
  (fn [request]
    (let [iteration (get-in request
                            [::sim-de-request/snapshot
                             ::sim-de-snapshot/iteration])]
      (-> (cond->
            request (< iteration n)
            (update ::sim-de-request/stop
                    conj
                    [{:cause ::nth-iteration
                      :iteration iteration
                      :n n}]))
          handler))))
