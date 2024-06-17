(ns
  automaton-simulation-de.simulation-engine.impl.middleware.request-validation-test
  (:require
   #?(:clj [clojure.test :refer [deftest is]]
      :cljs [cljs.test :refer [deftest is] :include-macros true])
   [automaton-core.adapters.schema
    :as core-schema]
   [automaton-simulation-de.simulation-engine
    :as-alias sim-engine]
   [automaton-simulation-de.simulation-engine.impl.middleware.request-validation
    :as sut]
   [automaton-simulation-de.simulation-engine.impl.stopping.cause
    :as sim-de-stopping-cause]))

(def ^:private event-stub
  #:automaton-simulation-de.simulation-engine{:type :a
                                              :date 1})

(deftest evaluates-test
  (is
   (=
    nil
    (->
      {::sim-engine/current-event event-stub
       ::sim-engine/event-execution (constantly {})
       :automaton-simulation-de.simulation-engine/snapshot
       #:automaton-simulation-de.simulation-engine{:id 1
                                                   :iteration 1
                                                   :date 1
                                                   :state {}
                                                   :past-events []
                                                   :future-events
                                                   [event-stub
                                                    #:automaton-simulation-de.simulation-engine{:type
                                                                                                :b
                                                                                                :date
                                                                                                2}]}
       ::sim-engine/sorting (constantly nil)
       ::sim-engine/stopping-causes []}
      sut/evaluates))
   "Well form request is not modifying the request.")
  (is (= nil
         (core-schema/validate-data-humanize sim-de-stopping-cause/schema
                                             (sut/evaluates nil))))
  (is
   (=
    nil
    (->
      {::sim-engine/current-event event-stub
       ::sim-engine/event-execution (constantly {})
       :automaton-simulation-de.simulation-engine/snapshot
       #:automaton-simulation-de.simulation-engine{:id 1
                                                   :iteration 1
                                                   :date 1
                                                   :state {}
                                                   :past-events []
                                                   :future-events
                                                   [event-stub
                                                    #:automaton-simulation-de.simulation-engine{:type
                                                                                                :b
                                                                                                :date
                                                                                                2}]}
       ::sim-engine/sorting (constantly nil)
       ::sim-engine/stopping-causes []}
      sut/evaluates))
   "Well form request is not modifying the request.")
  (is (= nil
         (core-schema/validate-data-humanize sim-de-stopping-cause/schema
                                             (sut/evaluates nil)))))
