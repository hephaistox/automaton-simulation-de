(ns automaton-simulation-de.simulation-engine.impl.middleware.state-rendering
  "Middleware to render state."
  (:require
   [automaton-simulation-de.simulation-engine :as-alias sim-engine]
   [clojure.string                            :as str]))

(defn wrap
  "Wrap the `handler` to  apply the rendering function `rendering-fn` to the state."
  [rendering-fn handler]
  (fn [request]
    (-> request
        (get-in [::sim-engine/snapshot ::sim-engine/state])
        rendering-fn)
    (handler request)))

(defn wrap-print
  "Wrap the `handler` to  apply the rendering function `rendering-fn` to the state."
  [rendering-fn handler]
  (fn [request]
    (println (str/join ","
                       (-> request
                           (get-in [::sim-engine/snapshot ::sim-engine/state])
                           rendering-fn)))
    (handler request)))
