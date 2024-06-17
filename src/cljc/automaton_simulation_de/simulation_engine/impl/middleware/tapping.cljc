#_{:heph-ignore {:forbidden-words ["tap>"]}}
(ns automaton-simulation-de.simulation-engine.impl.middleware.tapping
  "Middleware to tap response and requests.")

(defn wrap-response
  [handler]
  (fn [request]
    (let [response (handler request)]
      (tap> response)
      response)))

(defn wrap-request [handler] (fn [request] (tap> request) (handler request)))
