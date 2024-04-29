(ns automaton-simulation-de.rc.impl.preemption-policy.factory-test
  (:require
   [automaton-simulation-de.rc.impl.preemption-policy.factory :as sut]
   #?(:clj [clojure.test :refer [deftest is testing]]
      :cljs [cljs.test :refer [deftest is testing] :include-macros true])))

(deftest factory-test (testing "Defaulted" (is (some? (sut/factory {} nil)))))
