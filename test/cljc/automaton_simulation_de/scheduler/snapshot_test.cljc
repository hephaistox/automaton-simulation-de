(ns automaton-simulation-de.scheduler.snapshot-test
  (:require
   #?(:clj [clojure.test :refer [deftest is testing]]
      :cljs [cljs.test :refer [deftest is testing] :include-macros true])
   [automaton-core.adapters.schema                 :as core-schema]
   [automaton-simulation-de.ordering               :as sim-de-ordering]
   [automaton-simulation-de.scheduler.event        :as sim-de-event]
   [automaton-simulation-de.scheduler.event-return :as sim-de-event-return]
   [automaton-simulation-de.scheduler.snapshot     :as sut]))

(deftest schema-test
  (testing "Testing schema validity"
    (is (nil? (core-schema/validate-humanize (sut/schema)))))
  (testing "Test an example of iteration"
    (is (nil? (core-schema/validate-data-humanize (sut/schema)
                                                  {::sut/id 10
                                                   ::sut/iteration 10
                                                   ::sut/date 12
                                                   ::sut/state {}
                                                   ::sut/past-events []
                                                   ::sut/future-events []})))))

(deftest next-snapshot-test
  (testing "Nullable values"
    (is (= (sut/build 1 1 nil nil [] []) (sut/next-snapshot nil))))
  (testing
    "Expected evolution, current event is first future, new future head is dropped, snapshot increments iteration and id, date is the next current date"
    (is (= (sut/build 2
                      3
                      7
                      {:fo :bar}
                      (sim-de-event/make-events :a 1 :c 7)
                      (sim-de-event/make-events :b 12))
           (sut/next-snapshot (sut/build 1
                                         2
                                         3
                                         {:fo :bar}
                                         (sim-de-event/make-events :a 1)
                                         (sim-de-event/make-events :c 7
                                                                   :b 12))))))
  (testing "nil current-event is ok"
    (is (= (sut/build 2 3 3 {:fo :bar} (sim-de-event/make-events :a 1) [])
           (sut/next-snapshot
            (sut/build 1 2 3 {:fo :bar} (sim-de-event/make-events :a 1) nil)))))
  (testing "Useless data in snapshot are forgot"
    (is (= (sut/build 2
                      3
                      7
                      {:fo :bar}
                      (sim-de-event/make-events :a 1 :c 7)
                      (sim-de-event/make-events :b 12))
           (sut/next-snapshot (assoc (sut/build
                                      1
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
    (is (= {::sut/future-events (sim-de-event/make-events :a 1)
            ::sut/past-events []}
           (sut/inconsistency? {::sut/date 2
                                ::sut/future-events
                                (sim-de-event/make-events :b 20 :a 1)
                                ::sut/past-events []}))))
  (testing "Too late past events are returned as inconsistency"
    (is (= {::sut/past-events (sim-de-event/make-events :b 20)
            ::sut/future-events []}
           (sut/inconsistency? {::sut/date 2
                                ::sut/future-events []
                                ::sut/past-events
                                (sim-de-event/make-events :b 20 :a 1)})))))

(deftest update-snapshot-with-event-execution-test
  (testing
    "Event execution updates the snapshot, removes its state and future-events"
    (is
     (= (sut/build 1 1 1 {:foo :bar} [] (sim-de-event/make-events :a 1 :c 12))
        (sut/update-snapshot-with-event-return
         (sim-de-event-return/build {:foo :bar}
                                    (sim-de-event/make-events :a 1 :c 12))
         nil
         (sut/build 1 1 1 {:a :b} [] (sim-de-event/make-events :a 10 :b 12))))))
  (testing
    "Event execution updates the snapshot, removes its state and future-events"
    (is
     (= (sut/build 1 1 1 {:foo :bar} [] (sim-de-event/make-events :a 1 :c 12))
        (sut/update-snapshot-with-event-return
         (sim-de-event-return/build {:foo :bar}
                                    (sim-de-event/make-events :a 1 :c 12))
         nil
         (sut/build 1 1 1 {:a :b} [] (sim-de-event/make-events :a 10 :b 12))))))
  (testing "Future events are sorted"
    (is
     (= (sut/build 1 1 1 {:foo :bar} [] (sim-de-event/make-events :c 12 :a 20))
        (sut/update-snapshot-with-event-return
         (sim-de-event-return/build {:foo :bar}
                                    (sim-de-event/make-events :a 20 :c 12))
         (sim-de-ordering/sorter [(sim-de-ordering/compare-field
                                   ::sim-de-event/date)])
         (sut/build 1 1 1 {:a :b} [] (sim-de-event/make-events :a 10 :b 12))))))
  (testing "Future events are sorted"
    (is (= (sut/build 1 1 1 {:foo :bar} [] [])
           (sut/update-snapshot-with-event-return
            (sim-de-event-return/build {:foo :bar} nil)
            nil
            (sut/build 1 1 1 {:a :b} [] nil))))))
