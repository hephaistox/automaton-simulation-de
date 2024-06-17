(ns automaton-simulation-de.simulation-engine.impl.ordering.registry-test
  (:require
   #?(:clj [clojure.test :refer [deftest is testing]]
      :cljs [cljs.test :refer [deftest is testing] :include-macros true])
   [automaton-core.adapters.schema
    :as core-schema]
   [automaton-simulation-de.simulation-engine.impl.ordering.registry :as sut]))

(deftest schema-test (is (= nil (core-schema/validate-humanize sut/schema))))

(deftest registry-test
  (testing "Test built-in registry compliance to schema."
    (is (= nil (core-schema/validate-data-humanize sut/schema (sut/build))))))
