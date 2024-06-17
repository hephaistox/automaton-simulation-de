(ns
  automaton-simulation-de.simulation-engine.impl.built-in-sd.execution-not-found-test
  (:require
   #?(:clj [clojure.test :refer [deftest is]]
      :cljs [cljs.test :refer [deftest is] :include-macros true])
   [automaton-core.adapters.schema
    :as core-schema]
   [automaton-simulation-de.simulation-engine
    :as-alias sim-engine]
   [automaton-simulation-de.simulation-engine.impl.built-in-sd.execution-not-found
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

(deftest evaluates-test
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
       #:automaton-simulation-de.simulation-engine{:type :a
                                                   :date 12})
      (core-schema/validate-data-humanize sim-de-response/schema)))))
