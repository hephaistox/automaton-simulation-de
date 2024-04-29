(ns automaton-simulation-de.middleware.state-rendering
  "Middleware to render state"
  (:require
   [automaton-simulation-de.middleware.request :as sim-de-request]
   [automaton-simulation-de.scheduler.snapshot :as sim-de-snapshot]
   [clojure.string                             :as str]))

(defn state-rendering
  "Apply a rendering function to the state"
  [rendering-fn handler]
  (fn [request]
    (-> request
        (get-in [::sim-de-request/snapshot ::sim-de-snapshot/state])
        rendering-fn)
    (handler request)))

(defn state-printing
  "Apply a rendering function to the state"
  [rendering-fn handler]
  (fn [request]
    (println (str/join ","
                       (-> request
                           (get-in [::sim-de-request/snapshot
                                    ::sim-de-snapshot/state])
                           rendering-fn)))
    (handler request)))
