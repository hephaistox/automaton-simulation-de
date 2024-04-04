(ns automaton-simulation-de.scheduler.event-test
  (:require
   [automaton-core.adapters.schema          :as core-schema]
   #?(:clj [clojure.test :refer [deftest is testing]]
      :cljs [cljs.test :refer [deftest is testing] :include-macros true])
   [automaton-simulation-de.scheduler.event :as sut]))

(deftest schema-test
  (testing "Test schema of event"
    (is (nil? (core-schema/validate-humanize (sut/schema)))))
  (testing "Test valid events"
    (is (nil? (core-schema/validate-data-humanize (sut/schema)
                                                  (sut/make-event :a 12))))))

(deftest postpone-events-test
  (testing "Empty events are ok"
    (is (empty? (sut/postpone-events nil nil nil))))
  (testing "Example"
    (is (= (sut/make-events ::a 1 ::b 666 ::a 3 ::c 666)
           (sut/postpone-events (sut/make-events ::a 1 ::b 2 ::a 3 ::c 10)
                                (comp even? ::sut/date)
                                666))))
  (testing "None updated"
    (is (= (sut/make-events ::a 1 ::b 5 ::a 3 ::c 11)
           (sut/postpone-events (sut/make-events ::a 1 ::b 5 ::a 3 ::c 11)
                                (comp even? ::sut/date)
                                666)))))
