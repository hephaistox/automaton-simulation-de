(ns
  automaton-simulation-de.simulation-engine.impl.built-in-sd.failed-event-execution-test
  (:require
   #?(:clj [clojure.test :refer [deftest is]]
      :cljs [cljs.test :refer [deftest is] :include-macros true])
   [automaton-core.adapters.schema
    :as core-schema]
   [automaton-simulation-de.simulation-engine
    :as-alias sim-engine]
   [automaton-simulation-de.simulation-engine.impl.built-in-sd.failed-event-execution
    :as sut]
   [automaton-simulation-de.simulation-engine.impl.stopping.definition
    :as sim-de-sc-definition]
   [automaton-simulation-de.simulation-engine.response
    :as sim-de-response]))

(deftest stopping-definition-test
  (is (= nil
         (->> sut/stopping-definition
              (core-schema/validate-data-humanize
               sim-de-sc-definition/schema)))))

(deftest stopping-cause-test
  (is
   (=
    nil
    (->>
      (sut/evaluates
       #:automaton-simulation-de.simulation-engine{:stopping-causes []
                                                   :snapshot
                                                   #:automaton-simulation-de.simulation-engine{:id
                                                                                               1
                                                                                               :iteration
                                                                                               1
                                                                                               :date
                                                                                               1
                                                                                               :state
                                                                                               {}
                                                                                               :past-events
                                                                                               []
                                                                                               :future-events
                                                                                               []}}
       nil
       #:automaton-simulation-de.simulation-engine{:type :a
                                                   :date 1})
      (core-schema/validate-data-humanize sim-de-response/schema)))))
