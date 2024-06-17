(ns automaton-simulation-de.simulation-engine.impl.registry-test
  (:require
   #?(:clj [clojure.test :refer [deftest is testing]]
      :cljs [cljs.test :refer [deftest is testing] :include-macros true])
   [automaton-core.adapters.schema                          :as core-schema]
   [automaton-simulation-de.simulation-engine.impl.registry :as sut]))

(deftest schema-test
  (testing "registry is valid regarding the schema."
    (is (= nil (core-schema/validate-humanize sut/schema)))))

(deftest registry-test
  (testing "Default built-in registry is valid."
    (is (= nil (core-schema/validate-data-humanize sut/schema (sut/build))))))
