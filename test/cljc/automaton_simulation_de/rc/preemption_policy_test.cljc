(ns automaton-simulation-de.rc.preemption-policy-test
  (:require
   [automaton-core.adapters.schema               :as core-schema]
   #?(:clj [clojure.test :refer [deftest is testing]]
      :cljs [cljs.test :refer [deftest is testing] :include-macros true])
   [automaton-simulation-de.rc.preemption-policy :as sut]))

(deftest schema-test
  (testing "Schema is valid" (is (nil? (core-schema/validate-humanize (sut/schema))))))
