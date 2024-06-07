(ns automaton-simulation-de.orderings-test
  (:require
   [automaton-core.adapters.schema    :as core-schema]
   #?(:clj [clojure.test :refer [deftest is testing]]
      :cljs [cljs.test :refer [deftest is testing] :include-macros true])
   [automaton-simulation-de.orderings :as sut]))

(deftest schema-test (is (nil? (core-schema/validate-humanize sut/schema))))

(deftest validate-test
  (testing "Empty list is ok." (is (nil? (sut/validate [])))))
