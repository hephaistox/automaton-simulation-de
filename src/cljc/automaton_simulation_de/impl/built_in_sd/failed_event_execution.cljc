(ns automaton-simulation-de.impl.built-in-sd.failed-event-execution
  "`stopping-definition` to stop when an execution has raised an exception."
  (:require
   [automaton-simulation-de.response :as sim-de-response]))

(def stopping-definition
  {:id :failed-event-execution
   :next-possible? true
   :doc "Stops when an execution has raised an exception."})

(defn evaluates
  [response e current-event]
  (-> response
      (sim-de-response/add-stopping-cause
       {:stopping-criteria {:stopping-definition stopping-definition}
        :current-event current-event
        :context {:error e}})))
