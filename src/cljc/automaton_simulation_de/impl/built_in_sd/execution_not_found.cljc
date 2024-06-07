(ns automaton-simulation-de.impl.built-in-sd.execution-not-found
  "`stopping-definition` to stop when the execution of an event is not found in the registry."
  (:require
   [automaton-simulation-de.response        :as sim-de-response]
   [automaton-simulation-de.scheduler.event :as sim-de-event]))

(def stopping-definition
  {:id :execution-not-found
   :next-possible? true
   :doc "Stops when the execution of an event is not found in the registry."})

(defn evaluates
  [response event]
  (-> response
      (sim-de-response/add-stopping-cause
       {:stopping-criteria {:stopping-definition stopping-definition}
        :current-event event
        :context {:not-found-type (::sim-de-event/type event)}})))
