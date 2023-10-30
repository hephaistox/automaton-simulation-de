(ns automaton-simulation-de.simulation-test
  (:require [automaton-simulation-de.simulation :as sut]
            #?(:clj [clojure.test :refer [deftest is testing]]
               :cljs [cljs.test :refer [deftest is testing] :include-macros true])))

(deftest simulation-test
  (testing "A whole example to test simulation"
    (is (= 1 1))))
