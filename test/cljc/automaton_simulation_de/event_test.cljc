(ns automaton-simulation-de.event-test
  (:require
   [automaton-simulation-de.event :as sut]
   #?(:clj [clojure.test :refer [deftest is testing]]
      :cljs [cljs.test :refer [deftest is testing] :include-macros true])))

(deftest postpone-events-test
  (testing "Empty events are ok"
    (is (empty? (sut/postpone-events nil nil nil)))))
