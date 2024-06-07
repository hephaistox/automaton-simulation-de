(ns automaton-simulation-de.impl.registry-test
  (:require
   [automaton-core.adapters.schema        :as core-schema]
   #?(:clj [clojure.test :refer [deftest is testing]]
      :cljs [cljs.test :refer [deftest is testing] :include-macros true])
   [automaton-simulation-de.impl.registry :as sut]))

(deftest schema-test
  (testing "registry is valid regarding the schema."
    (is (nil? (core-schema/validate-humanize sut/schema)))))

(deftest registry-test
  (testing "Default built-in registry is valid."
    (is (nil? (sut/validate (sut/build))))))
