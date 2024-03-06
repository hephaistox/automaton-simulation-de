(ns automaton-simulation-de.scheduler.iteration-test
  (:require
   [automaton-simulation-de.scheduler.iteration :as sut]
   #?(:clj [clojure.test :refer [deftest is testing]]
      :cljs [cljs.test :refer [deftest is testing] :include-macros true])))

(deftest scheduler-iteration-execute-test
  (testing (is (= {:id 2
                   :past-events [{:type :ca
                                  :date 3}]
                   :state {}
                   :future-events [{:type :ba
                                    :date 2}]}
                  (sut/execute {}
                               (constantly true)
                               {:id 1
                                :state {}
                                :past-events []
                                :future-events [{:type :ba
                                                 :date 2}
                                                {:type :ca
                                                 :date 3}]})))))
