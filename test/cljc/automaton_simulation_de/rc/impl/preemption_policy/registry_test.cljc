(ns automaton-simulation-de.rc.impl.preemption-policy.registry-test
  (:require
   [automaton-core.adapters.schema                             :as core-schema]
   #?(:clj [clojure.test :refer [deftest is testing]]
      :cljs [cljs.test :refer [deftest is testing] :include-macros true])
   [automaton-simulation-de.rc.impl.preemption-policy.registry :as sut]))

(deftest registry-test
  (testing "Validate registry"
    (is (nil? (core-schema/validate-data-humanize (sut/schema) (sut/registry))))))
