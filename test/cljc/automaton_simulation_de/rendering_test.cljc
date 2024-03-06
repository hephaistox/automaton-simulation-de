(ns automaton-simulation-de.rendering-test
  (:require
   [automaton-simulation-de.rendering :as sut]
   #?(:clj [clojure.test :refer [deftest is testing]]
      :cljs [cljs.test :refer [deftest is testing] :include-macros true])))

(deftest evt-str-test
  (testing "Test a simple event"
    (is (= "foo(1)"
           (sut/evt-str {:type :foo
                         :date 1}))))
  (testing "Test a simple event" (is (= "()" (sut/evt-str nil)))))

(deftest evt-str-with-data-test
  (testing "Test a simple event"
    (is (= "foo(1)"
           (sut/evt-with-data-str {:type :foo
                                   :date 1}))))
  (testing "Test a simple event"
    (is (= "()" (sut/evt-with-data-str nil) (sut/evt-with-data-str {}))))
  (testing "Test a simple event"
    (is (= "foo(1,bar,a)"
           (sut/evt-with-data-str {:type :foo
                                   :date 1
                                   :some :bar
                                   :more-data :a})))))
