(ns
  automaton-simulation-de.simulation-engine.impl.built-in-sd.execution-not-found
  "`stopping-definition` to stop when the execution of an event is not found in the registry."
  (:require
   [automaton-simulation-de.simulation-engine          :as-alias sim-engine]
   [automaton-simulation-de.simulation-engine.response :as sim-de-response]))

(def stopping-definition
  #:automaton-simulation-de.simulation-engine{:id
                                              ::sim-engine/execution-not-found
                                              :next-possible? true
                                              :doc
                                              "Stops when the execution of an event is not found in the registry."})

(defn evaluates
  [response event]
  (->
    response
    (sim-de-response/add-stopping-cause
     #:automaton-simulation-de.simulation-engine{:stopping-criteria
                                                 #:automaton-simulation-de.simulation-engine{:stopping-definition
                                                                                             stopping-definition}
                                                 :current-event event
                                                 :context
                                                 #:automaton-simulation-de.simulation-engine{:not-found-type
                                                                                             (::sim-engine/type
                                                                                              event)}})))
