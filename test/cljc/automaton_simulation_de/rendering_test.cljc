(ns automaton-simulation-de.rendering-test
  (:require
   [automaton-simulation-de.rendering       :as sut]
   [automaton-simulation-de.scheduler.event :as sim-de-event]
   #?(:clj [clojure.test :refer [deftest is testing]]
      :cljs [cljs.test :refer [deftest is testing] :include-macros true])))

(deftest evt-str-test
  (testing "Test a simple event"
    (is (= "foo(1)" (sut/evt-str (sim-de-event/make-event :foo 1)))))
  (testing "Test a simple event" (is (= "()" (sut/evt-str nil)))))

(deftest evt-with-data-str-test
  (testing "Test a simple event"
    (is (= "foo(1)" (sut/evt-with-data-str (sim-de-event/make-event :foo 1)))))
  (testing "Test a simple event"
    (is (= "()" (sut/evt-with-data-str nil) (sut/evt-with-data-str {}))))
  (testing "Test a simple event"
    (is (= "foo(1,a)"
           (sut/evt-with-data-str
            (assoc (sim-de-event/make-event :foo 1) :bar "a"))))))
