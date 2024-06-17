(ns
  automaton-simulation-de.simulation-engine.impl.built-in-sd.failed-event-execution
  "`stopping-definition` to stop when an execution has raised an exception."
  (:require
   [automaton-simulation-de.simulation-engine          :as-alias sim-engine]
   [automaton-simulation-de.simulation-engine.response :as sim-de-response]))

(def stopping-definition
  #:automaton-simulation-de.simulation-engine{:id
                                              :automaton-simulation-de.simulation-engine/failed-event-execution
                                              :next-possible? true
                                              :doc
                                              "Stops when an execution has raised an exception."})

(defn evaluates
  [response e current-event]
  (->
    response
    (sim-de-response/add-stopping-cause
     #:automaton-simulation-de.simulation-engine{:stopping-criteria
                                                 #:automaton-simulation-de.simulation-engine{:stopping-definition
                                                                                             stopping-definition}
                                                 :current-event current-event
                                                 :context
                                                 #:automaton-simulation-de.simulation-engine{:error
                                                                                             e}})))
