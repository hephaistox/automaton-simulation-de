(ns automaton-simulation-de.impl.built-in-sd.response-validation
  "Stops when the response is not valid.")

(defn stopping-definition
  []
  {:doc "Stops when the response is not valid."
   :id :response-schema
   :next-possible? true})
