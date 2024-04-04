(ns automaton-simulation-de.middleware.schema-validation-test
  (:require
   #?(:clj [clojure.test :refer [deftest is testing]]
      :cljs [cljs.test :refer [deftest is testing] :include-macros true])
   [automaton-simulation-de.middleware.request           :as sim-de-request]
   [automaton-simulation-de.middleware.response          :as sim-de-response]
   [automaton-simulation-de.middleware.schema-validation :as sut]
   [automaton-simulation-de.ordering                     :as
                                                         sim-de-event-ordering]
   [automaton-simulation-de.scheduler.event              :as sim-de-event]
   [automaton-simulation-de.scheduler.snapshot           :as sim-de-snapshot]))

(deftest request-validation-test
  (testing "Valid request is accepted"
    (is (empty? (-> ((sut/request-validation (fn [request] request))
                     (sim-de-request/build
                      nil
                      (constantly {})
                      (sim-de-snapshot/build 1
                                             1
                                             1
                                             {}
                                             (sim-de-event/make-events :a 1)
                                             (sim-de-event/make-events :a 1))
                      (sim-de-event-ordering/sorter
                       [(sim-de-event-ordering/compare-types [:a :b])])
                      []))
                    ::sim-de-request/stop))))
  (testing "Inconsistent request is detected"
    (let [request (sim-de-request/build
                   nil
                   (constantly {})
                   (sim-de-snapshot/build 1 1 nil nil nil nil)
                   []
                   [])]
      (is (= [{:cause ::sut/request-inconsistency
               :request request
               :error ::sim-de-snapshot/nil-date}]
             (-> ((sut/request-validation (fn [request] request)) request)
                 ::sim-de-response/stop)))))
  (testing "Invalid request is detected"
    (is (seq? (-> ((sut/request-validation (fn [request] request)) {:bouh :fo})
                  ::sim-de-request/stop)))))

(deftest response-validation-test
  (testing "Valid response is accepted"
    (is (empty? (-> ((sut/response-validation
                      (fn [_]
                        (sim-de-response/build
                         []
                         (sim-de-snapshot/build 1 1 1 nil [] []))))
                     nil)
                    ::sim-de-response/stop))))
  (testing "Inconsistent response is detected"
    (is (= [{:cause ::sut/response-inconsistency
             :response (sim-de-response/build
                        []
                        (sim-de-snapshot/build 1 1 nil nil nil nil))
             :error ::sim-de-snapshot/nil-date}]
           (-> ((sut/response-validation
                 (fn [_]
                   (sim-de-response/build
                    []
                    (sim-de-snapshot/build 1 1 nil nil nil nil))))
                nil)
               ::sim-de-response/stop)))
    (is
     (=
      (-> ((sut/response-validation (fn [_]
                                      (sim-de-response/build
                                       []
                                       (sim-de-snapshot/build
                                        1
                                        1
                                        1
                                        nil
                                        (sim-de-event/make-events :a 2)
                                        (sim-de-event/make-events :a 0)))))
           nil)
          ::sim-de-response/stop)
      [{:cause ::sut/response-inconsistency
        :response (sim-de-response/build []
                                         (sim-de-snapshot/build
                                          1
                                          1
                                          1
                                          nil
                                          (sim-de-event/make-events :a 2)
                                          (sim-de-event/make-events :a 0)))
        :error
        #:automaton-simulation-de.scheduler.snapshot{:future-events
                                                     [#:automaton-simulation-de.scheduler.event{:type
                                                                                                :a
                                                                                                :date
                                                                                                0}]
                                                     :past-events
                                                     [#:automaton-simulation-de.scheduler.event{:type
                                                                                                :a
                                                                                                :date
                                                                                                2}]}}])))
  (testing "Invalid request is detected"
    (is (seq? (-> ((sut/response-validation (fn [_] {})) nil)
                  ::sim-de-response/stop)))))
