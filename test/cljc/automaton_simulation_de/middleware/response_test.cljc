(ns automaton-simulation-de.middleware.response-test
  (:require
   #?(:clj [clojure.test :refer [deftest is testing]]
      :cljs [cljs.test :refer [deftest is testing] :include-macros true])
   [automaton-core.adapters.schema              :as core-schema]
   [automaton-simulation-de.middleware.response :as sut]
   [automaton-simulation-de.scheduler.event     :as sim-de-event]
   [automaton-simulation-de.scheduler.snapshot  :as sim-de-snapshot]))

(deftest schema-test
  (testing "Test schema"
    (is (nil? (core-schema/validate-humanize (sut/schema))))))

(deftest build-response-test
  (testing "Test build response with empty values" (is (sut/build [] {})))
  (testing "Test build response with nil values" (is (sut/build nil nil))))

(deftest prepare-reponse-test
  (testing "Nil values are accepted in response"
    (is (= (sut/build [] (sim-de-snapshot/build 1 1 nil nil [] []))
           (sut/prepare nil nil))))
  (testing
    "No future events implies no modification of date, but the incrementation of iteration and id"
    (is (= (sim-de-snapshot/build 4 4 10 {} [] [])
           (-> (sut/prepare (sim-de-snapshot/build 3 3 10 {} [] []) [])
               ::sut/snapshot))))
  (testing
    "If the future event is happening in the past, then it breaks causality"
    (is
     (= [{:cause ::sut/causality-broken
          :previous-date 10
          :next-date 1}]
        (->
          (sut/prepare
           (sim-de-snapshot/build 3 3 10 {} [] (sim-de-event/make-events :a 1))
           nil)
          ::sut/stop))))
  (testing
    "A future event at the same date than the current snapshot or later on is possible"
    (is
     (empty?
      (-> (sut/prepare
           (sim-de-snapshot/build 3 3 10 {} [] (sim-de-event/make-events :a 13))
           nil)
          ::sut/stop)))
    (is
     (empty?
      (-> (sut/prepare
           (sim-de-snapshot/build 3 3 10 {} [] (sim-de-event/make-events :a 10))
           nil)
          ::sut/stop)))))
