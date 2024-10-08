(ns automaton-simulation-de.simulation-engine.impl.stopping.definition-test
  (:require
   #?(:clj [clojure.test :refer [deftest is testing]]
      :cljs [cljs.test :refer [deftest is testing] :include-macros true])
   [automaton-core.adapters.schema                                     :as core-schema]
   [automaton-simulation-de.simulation-engine                          :as-alias sim-engine]
   [automaton-simulation-de.simulation-engine.impl.stopping.definition :as sut]))

(deftest id-schema-test (is (= nil (core-schema/validate-humanize sut/id-schema))))

(deftest schema-test
  (testing "`stopping-evaluation` are optional."
    (is (= nil
           (core-schema/validate-data-humanize
            sut/schema
            #:automaton-simulation-de.simulation-engine{:doc ""
                                                        :id :foo
                                                        :next-possible? true})))
    (is (= nil
           (core-schema/validate-data-humanize
            sut/schema
            #:automaton-simulation-de.simulation-engine{:doc ""
                                                        :id :foo
                                                        :stopping-evaluation (constantly true)
                                                        :next-possible? true}))))
  (is (= nil (core-schema/validate-humanize sut/schema))))
