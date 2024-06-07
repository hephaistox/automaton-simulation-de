(ns automaton-simulation-de.scheduler.snapshot-test
  (:require
   #?(:clj [clojure.test :refer [deftest is testing]]
      :cljs [cljs.test :refer [deftest is testing] :include-macros true])
   [automaton-core.adapters.schema             :as core-schema]
   [automaton-simulation-de.scheduler.event    :as sim-de-event]
   [automaton-simulation-de.scheduler.snapshot :as sut]))

(deftest schema-test
  (testing "Testing schema validity"
    (is (nil? (core-schema/validate-humanize sut/schema))))
  (testing "Test an example of iteration"
    (is (nil? (sut/validate {::sut/id 10
                             ::sut/iteration 10
                             ::sut/date 12
                             ::sut/state {}
                             ::sut/past-events []
                             ::sut/future-events []})))))

(deftest consume-first-event-test
  (testing "Nullable values"
    (is (= (sut/build 1 nil nil nil [] []) (sut/consume-first-event nil))))
  (testing
    "As expected, current event is first future, new future head is dropped, snapshot and id, date is the next current date"
    (is (= (sut/build 2
                      2
                      7
                      {:fo :bar}
                      (sim-de-event/make-events :a 1 :c 7)
                      (sim-de-event/make-events :b 12))
           (sut/consume-first-event (sut/build
                                     1
                                     2
                                     3
                                     {:fo :bar}
                                     (sim-de-event/make-events :a 1)
                                     (sim-de-event/make-events :c 7 :b 12))))))
  (testing "nil current-event is ok"
    (is (= (sut/build 2 2 3 {:fo :bar} (sim-de-event/make-events :a 1) [])
           (sut/consume-first-event
            (sut/build 1 2 3 {:fo :bar} (sim-de-event/make-events :a 1) nil)))))
  (testing "Useless data in snapshot are forgot"
    (is (= (sut/build 2
                      2
                      7
                      {:fo :bar}
                      (sim-de-event/make-events :a 1 :c 7)
                      (sim-de-event/make-events :b 12))
           (sut/consume-first-event
            (assoc (sut/build 1
                              2
                              3
                              {:fo :bar}
                              (sim-de-event/make-events :a 1)
                              (sim-de-event/make-events :c 7 :b 12))
                   :foo
                   :bar))))))

(deftest inconsistency?-test
  (testing "Empty past and future are consistent"
    (is (false? (sut/inconsistency? {::sut/date 2
                                     ::sut/future-events []
                                     ::sut/past-events []}))))
  (testing
    "Past events before 5, snapshot at 5, and future after 5 are seen as consistent"
    (is (false? (sut/inconsistency?
                 {::sut/date 5
                  ::sut/future-events (sim-de-event/make-events :b 20 :a 7 :b 5)
                  ::sut/past-events (sim-de-event/make-events :b 4 :a 5)}))))
  (testing "Too early future events are returned as inconsistency"
    (is (= {:snapshot-date 2
            :mismatching-events {::sut/future-events
                                 (sim-de-event/make-events :a 1)
                                 ::sut/past-events []}}
           (sut/inconsistency? {::sut/date 2
                                ::sut/future-events
                                (sim-de-event/make-events :b 20 :a 1)
                                ::sut/past-events []}))))
  (testing "Too late past events are returned as inconsistency"
    (is (= {:snapshot-date 2
            :mismatching-events {::sut/past-events
                                 (sim-de-event/make-events :b 20)
                                 ::sut/future-events []}}
           (sut/inconsistency? {::sut/date 2
                                ::sut/future-events []
                                ::sut/past-events
                                (sim-de-event/make-events :b 20 :a 1)})))))
