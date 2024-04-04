(ns automaton-simulation-de.scheduler-test
  (:require
   #?(:clj [clojure.test :refer [deftest is testing]]
      :cljs [cljs.test :refer [deftest is testing] :include-macros true])
   [automaton-simulation-de.middleware.request     :as sim-de-request]
   [automaton-simulation-de.middleware.response    :as sim-de-response]
   [automaton-simulation-de.ordering               :as sim-de-event-ordering]
   [automaton-simulation-de.registry               :as sim-de-event-registry]
   [automaton-simulation-de.scheduler              :as sut]
   [automaton-simulation-de.scheduler.event        :as sim-de-event]
   [automaton-simulation-de.scheduler.event-return :as sim-de-event-return]
   [automaton-simulation-de.scheduler.snapshot     :as sim-de-snapshot]))

(deftest handler-test
  (testing "No event execution is stopping the execution"
    (is (= (sim-de-response/build
            [{:cause ::sim-de-event-registry/execution-not-found
              :not-found-type nil}]
            (sim-de-snapshot/build 3 3 2 {:foo :bar} [] []))
           (sut/handler (sim-de-request/build
                         nil
                         nil
                         (sim-de-snapshot/build 2 2 2 {:foo :bar} nil nil)
                         nil
                         [])))))
  (testing "Stop in the request is passed to the response"
    (is (= [{:cause ::test-stopping
             :current-event nil}]
           (-> (sut/handler (sim-de-request/build nil
                                                  (constantly {})
                                                  nil
                                                  nil
                                                  [{:cause ::test-stopping}]))
               ::sim-de-response/stop))))
  (testing "Data added in event execution are in the response"
    (is (= {:foo3 :bar3}
           (-> (sut/handler (sim-de-request/build
                             nil
                             (constantly
                              (sim-de-event-return/build {:foo3 :bar3} []))
                             nil
                             nil
                             []))
               (get-in [::sim-de-response/snapshot ::sim-de-snapshot/state])))))
  (testing "An early event is first in the future list"
    (is (= (sim-de-event/make-events :a 12 :b 13 :d 15)
           (-> (sut/handler
                (sim-de-request/build
                 nil
                 (constantly (sim-de-event-return/build
                              {:foo3 :bar3}
                              (sim-de-event/make-events :a 12 :d 15 :b 13)))
                 (sim-de-snapshot/build 1 1 1 nil nil nil)
                 (sim-de-event-ordering/sorter
                  [(sim-de-event-ordering/compare-field ::sim-de-event/date)])
                 []))
               (get-in [::sim-de-response/snapshot
                        ::sim-de-snapshot/future-events])))))
  (testing
    "Handler raising an exception is skipped, next snapshot is returned, with failing event in the past"
    (is
     (= (sim-de-response/build [{:cause ::sut/failed-event-execution
                                 :error nil}]
                               (sim-de-snapshot/build
                                2
                                2
                                12
                                {:foo3 :bar3}
                                (sim-de-event/make-events :a 10 :b 11 :aa 12)
                                (sim-de-event/make-events :bb 14)))
        (-> (sut/handler
             (sim-de-request/build
              nil
              (fn [_ _ _] (throw (ex-info "Arg" {})))
              (sim-de-snapshot/build 1
                                     1
                                     1
                                     {:foo3 :bar3}
                                     (sim-de-event/make-events :a 10 :b 11)
                                     (sim-de-event/make-events :aa 12 :bb 14))
              (sim-de-event-ordering/sorter
               [(sim-de-event-ordering/compare-field ::sim-de-event/date)])
              []))
            (assoc-in [::sim-de-response/stop 0 :error] nil)))))
  (testing "An event happened in the past"
    (is
     (=
      [{:cause ::sim-de-response/causality-broken
        :previous-date 10
        :next-date 5}]
      (->
        (sut/handler
         (sim-de-request/build
          nil
          (constantly (sim-de-event-return/build
                       {:foo3 :bar3}
                       (sim-de-event/make-events :a 5 :a 12 :d 15 :b 13)))
          (sim-de-snapshot/build 1 1 10 nil nil (sim-de-event/make-events :a 5))
          (sim-de-event-ordering/sorter [])
          []))
        (get ::sim-de-response/stop))))))

(deftest iterate-test
  (testing "Nil values are ok"
    (is (= (sim-de-response/build [{:cause ::sim-de-request/no-future-events}
                                   {:cause ::sut/nil-handler}]
                                  (sim-de-snapshot/build 1 1 nil nil [] []))
           (sut/iterate nil nil nil nil))))
  (testing "Nil handler is detected"
    (is (= [{:cause ::sut/nil-handler}]
           (-> (sut/iterate nil
                            nil
                            nil
                            (sim-de-snapshot/build
                             1
                             1
                             1
                             nil
                             nil
                             (sim-de-event/make-events :a 10 :b 12)))
               (get-in [::sim-de-response/stop])))))
  (testing
    "First event is properly executed, state and future events are up to date"
    (is
     (= (sim-de-response/build []
                               (sim-de-snapshot/build
                                2
                                2
                                10
                                {:a :b
                                 :c :d}
                                (sim-de-event/make-events :a 10)
                                (sim-de-event/make-events :b 12 :a 13 :b 14)))
        (sut/iterate
         {:a (fn [_ state future-events]
               (sim-de-event-return/build
                (assoc state :c :d)
                (concat future-events (sim-de-event/make-events :a 13 :b 14))))}
         nil
         sut/handler
         (sim-de-snapshot/build 1
                                1
                                1
                                {:a :b}
                                []
                                (sim-de-event/make-events :a 10 :b 12)))))
    (is
     (= (sim-de-response/build []
                               (sim-de-snapshot/build
                                2
                                2
                                10
                                {:a :b
                                 :c :d}
                                (sim-de-event/make-events :a 10)
                                []))
        (sut/iterate
         {:a (fn [_ state future-events]
               (sim-de-event-return/build (assoc state :c :d) future-events))}
         nil
         sut/handler
         (sim-de-snapshot/build 1
                                1
                                1
                                {:a :b}
                                []
                                (sim-de-event/make-events :a 10))))))
  (testing "End to end state and future events path"
    (is
     (= {::sim-de-snapshot/future-events (sim-de-event/make-events :a 13 :b 14)
         ::sim-de-snapshot/state {:a :b}}
        (-> (sut/iterate {:a (fn [_ _ _]
                               (sim-de-event-return/build
                                {:a :b}
                                (sim-de-event/make-events :a 13 :b 14)))}
                         nil
                         sut/handler
                         (sim-de-snapshot/build
                          1
                          1
                          1
                          {}
                          []
                          (sim-de-event/make-events :a 10 :b 12)))
            ::sim-de-response/snapshot
            (select-keys [::sim-de-snapshot/future-events
                          ::sim-de-snapshot/state]))))))

(deftest scheduler-test
  (testing "No data is ok"
    (is (= (sim-de-response/build [{:cause ::sim-de-request/no-future-events
                                    :current-event nil}]
                                  (sim-de-snapshot/build 1 1 nil nil [] []))
           (sut/scheduler nil [] nil nil 100))))
  (testing "A single execution is ok"
    (is
     (= (sim-de-response/build [{:cause ::sim-de-request/no-future-events
                                 :current-event nil}]
                               (sim-de-snapshot/build
                                3
                                3
                                10
                                {:foo 1
                                 :evt :a}
                                (sim-de-event/make-events :a 10)
                                []))
        (sut/scheduler
         {:a (fn [_ state future-events]
               (sim-de-event-return/build (assoc state :evt :a) future-events))}
         []
         [(sim-de-event-ordering/compare-field ::sim-de-event/date)]
         (sim-de-snapshot/build 1
                                1
                                1
                                {:foo 1}
                                nil
                                (sim-de-event/make-events :a 10))
         10))))
  (testing "Looping of 3 iterations is ok"
    (is
     (=
      (sim-de-response/build [{:cause ::sim-de-request/no-future-events
                               :current-event nil}]
                             (sim-de-snapshot/build
                              5
                              5
                              12
                              {:foo 1
                               :evt-a 1
                               :evt-b 1
                               :evt-c 1}
                              (sim-de-event/make-events :a 10 :c 10 :b 12)
                              []))
      (sut/scheduler
       {:a (fn [_ state future-events]
             (sim-de-event-return/build
              (update state :evt-a (fnil inc 0))
              (concat future-events (sim-de-event/make-events :b 12 :c 10))))
        :b (fn [_ state future-events]
             (sim-de-event-return/build (update state :evt-b (fnil inc 0))
                                        future-events))
        :c (fn [_ state future-events]
             (sim-de-event-return/build (update state :evt-c (fnil inc 0))
                                        future-events))}
       nil
       [(sim-de-event-ordering/compare-field ::sim-de-event/date)
        (sim-de-event-ordering/compare-types [:a :b :c])]
       (sim-de-snapshot/build 1
                              1
                              1
                              {:foo 1}
                              nil
                              (sim-de-event/make-events :a 10))
       10)))))
