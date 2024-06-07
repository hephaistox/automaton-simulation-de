(ns automaton-simulation-de.impl.middleware.state-rendering
  "Middleware to render state."
  (:require
   [automaton-simulation-de.request            :as sim-de-request]
   [automaton-simulation-de.scheduler.snapshot :as sim-de-snapshot]
   [clojure.string                             :as str]))

(defn wrap
  "Wrap the `handler` to  apply the rendering function `rendering-fn` to the state."
  [rendering-fn handler]
  (fn [request]
    (-> request
        (get-in [::sim-de-request/snapshot ::sim-de-snapshot/state])
        rendering-fn)
    (handler request)))

(defn wrap-print
  "Wrap the `handler` to  apply the rendering function `rendering-fn` to the state."
  [rendering-fn handler]
  (fn [request]
    (println (str/join ","
                       (-> request
                           (get-in [::sim-de-request/snapshot
                                    ::sim-de-snapshot/state])
                           rendering-fn)))
    (handler request)))
