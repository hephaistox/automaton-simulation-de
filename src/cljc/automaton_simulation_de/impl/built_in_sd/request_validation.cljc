(ns automaton-simulation-de.impl.built-in-sd.request-validation
  "Stops when the request is not valid.")

(defn stopping-definition
  []
  {:doc "Stops when the request is not valid."
   :id :request-schema
   :next-possible? true})
