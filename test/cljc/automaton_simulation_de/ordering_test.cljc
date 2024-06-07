(ns automaton-simulation-de.ordering-test
  (:require
   #?(:clj [clojure.test :refer [deftest is testing]]
      :cljs [cljs.test :refer [deftest is testing] :include-macros true])
   [automaton-core.adapters.schema          :as core-schema]
   [automaton-simulation-de.ordering        :as sut]
   [automaton-simulation-de.scheduler.event :as sim-de-event]))

(deftest schema-test (is (nil? (core-schema/validate-humanize sut/schema))))

(deftest compare-field-test
  (testing "Test ordering with integer."
    (is (neg? (apply (sut/compare-field ::sim-de-event/date)
                     (sim-de-event/make-events :a 1 :a 2))))
    (is (neg? (apply (sut/compare-field ::sim-de-event/date)
                     (sim-de-event/make-events :a 1 :a 10))))
    (is (pos? (apply (sut/compare-field ::sim-de-event/date)
                     (sim-de-event/make-events :a 10 :a 1))))
    (is (zero? (apply (sut/compare-field ::sim-de-event/date)
                      (sim-de-event/make-events :a 1 :a 1)))))
  (testing "Nil values are pushed to the end."
    (is (neg? ((sut/compare-field ::sim-de-event/date)
               (sim-de-event/make-event :a 1)
               nil)))
    (is (pos? ((sut/compare-field ::sim-de-event/date)
               nil
               (sim-de-event/make-event :a 1))))))

(deftest compare-types-test
  (testing "Found values."
    (is (neg? (apply (sut/compare-types [:a :b :c])
                     (sim-de-event/make-events :a 0 :b 0))))
    (is (neg? (apply (sut/compare-types [:a :b :c])
                     (sim-de-event/make-events :a 0 :c 0))))
    (is (pos? (apply (sut/compare-types [:a :b :c])
                     (sim-de-event/make-events :c 0 :a 0))))
    (is (pos? (apply (sut/compare-types [:a :b :c])
                     (sim-de-event/make-events :b 0 :a 0)))))
  (testing "Nil values are pushed to the end."
    (is (neg?
         ((sut/compare-types [:a :b :c]) (sim-de-event/make-event :a 1) nil)))
    (is (pos?
         ((sut/compare-types [:a :b :c]) nil (sim-de-event/make-event :a 1))))))

(deftest sorter-test
  (testing "Are dates sorted."
    (is (= [:a :b]
           (->> ((sut/sorter [(sut/compare-types [:a :b :c])])
                 (sim-de-event/make-events :a 1 :b 2))
                (mapv ::sim-de-event/type))))
    (is (= [:a :b]
           (->> ((sut/sorter [(sut/compare-types [:a :b :c])])
                 (sim-de-event/make-events :b 2 :a 1))
                (mapv ::sim-de-event/type))))))


(deftest data-to-fn-test
  (is ((sut/data-to-fn [:field :product])
       (sim-de-event/make-event :product 0)
       (sim-de-event/make-event :product 1))))
