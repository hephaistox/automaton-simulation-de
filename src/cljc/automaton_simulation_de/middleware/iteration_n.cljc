(ns automaton-simulation-de.middleware.iteration-n
  "Iterates until a given iteration number"
  (:require
   [automaton-simulation-de.middleware.request :as sim-de-request]
   [automaton-simulation-de.scheduler.snapshot :as sim-de-snapshot]))

(defn wrap-iteration-n
  "Stop after iteration n

  Writes `::stop` in the request under the `::scheduler-middleware-request/stop` keyword

  Params:
  * `n` the number of iteration
  * `handler`"
  [n handler]
  (fn [request]
    (let [iteration (get-in request
                            [::sim-de-request/snapshot
                             ::sim-de-snapshot/iteration])]
      (-> (cond-> request
            (< iteration n) (update ::sim-de-request/stop conj
                                    [{:cause ::nth-iteration
                                      :iteration iteration
                                      :n n}]))
          handler))))
