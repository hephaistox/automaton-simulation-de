(ns automaton-simulation-de.impl.model-test
  (:require
   #?(:clj [clojure.test :refer [deftest is testing]]
      :cljs [cljs.test :refer [deftest is testing] :include-macros true])
   [automaton-core.adapters.schema     :as core-schema]
   [automaton-simulation-de.impl.model :as sut]))

(deftest schema-test
  (testing "Schema is valid"
    (is (nil? (core-schema/validate-humanize (sut/schema))))))
