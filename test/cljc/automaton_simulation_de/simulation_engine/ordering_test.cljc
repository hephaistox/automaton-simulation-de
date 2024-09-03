(ns automaton-simulation-de.simulation-engine.ordering-test
  (:require
   #?(:clj [clojure.test :refer [deftest is testing]]
      :cljs [cljs.test :refer [deftest is testing] :include-macros true])
   [automaton-core.adapters.schema                     :as core-schema]
   [automaton-simulation-de.simulation-engine          :as-alias sim-engine]
   [automaton-simulation-de.simulation-engine.ordering :as sut]))

(deftest schema-test (is (= nil (core-schema/validate-humanize sut/schema))))

(deftest compare-field-test
  (testing "Test ordering with integer."
    (is (neg? (apply (sut/compare-field ::sim-engine/date)
                     [#:automaton-simulation-de.simulation-engine{:type :a
                                                                  :date 1}
                      #:automaton-simulation-de.simulation-engine{:type :a
                                                                  :date 2}])))
    (is (neg? (apply (sut/compare-field ::sim-engine/date)
                     [#:automaton-simulation-de.simulation-engine{:type :a
                                                                  :date 1}
                      #:automaton-simulation-de.simulation-engine{:type :a
                                                                  :date 10}])))
    (is (pos? (apply (sut/compare-field ::sim-engine/date)
                     [#:automaton-simulation-de.simulation-engine{:type :a
                                                                  :date 10}
                      #:automaton-simulation-de.simulation-engine{:type :a
                                                                  :date 1}])))
    (is (zero? (apply (sut/compare-field ::sim-engine/date)
                      [#:automaton-simulation-de.simulation-engine{:type :a
                                                                   :date 1}
                       #:automaton-simulation-de.simulation-engine{:type :a
                                                                   :date 1}]))))
  (testing "Nil values are pushed to the end."
    (is (neg? ((sut/compare-field ::sim-engine/date)
               #:automaton-simulation-de.simulation-engine{:type :a
                                                           :date 1}
               nil)))
    (is (pos? ((sut/compare-field ::sim-engine/date)
               nil
               #:automaton-simulation-de.simulation-engine{:type :a
                                                           :date 1})))))

(deftest compare-types-test
  (testing "Found values."
    (is (neg? (apply (sut/compare-types [:a :b :c])
                     [#:automaton-simulation-de.simulation-engine{:type :a
                                                                  :date 0}
                      #:automaton-simulation-de.simulation-engine{:type :b
                                                                  :date 0}])))
    (is (neg? (apply (sut/compare-types [:a :b :c])
                     [#:automaton-simulation-de.simulation-engine{:type :a
                                                                  :date 0}
                      #:automaton-simulation-de.simulation-engine{:type :c
                                                                  :date 0}])))
    (is (pos? (apply (sut/compare-types [:a :b :c])
                     [#:automaton-simulation-de.simulation-engine{:type :c
                                                                  :date 0}
                      #:automaton-simulation-de.simulation-engine{:type :a
                                                                  :date 0}])))
    (is (pos? (apply (sut/compare-types [:a :b :c])
                     [#:automaton-simulation-de.simulation-engine{:type :b
                                                                  :date 0}
                      #:automaton-simulation-de.simulation-engine{:type :a
                                                                  :date 0}]))))
  (testing "Nil values are pushed to the end."
    (is (neg? ((sut/compare-types [:a :b :c])
               #:automaton-simulation-de.simulation-engine{:type :a
                                                           :date 1}
               nil)))
    (is (pos? ((sut/compare-types [:a :b :c])
               nil
               #:automaton-simulation-de.simulation-engine{:type :a
                                                           :date 1})))))

(deftest sorter-test
  (testing "Are dates sorted."
    (is (= [:a :b]
           (->> ((sut/sorter [(sut/compare-types [:a :b :c])])
                 [#:automaton-simulation-de.simulation-engine{:type :a
                                                              :date 1}
                  #:automaton-simulation-de.simulation-engine{:type :b
                                                              :date 2}])
                (mapv ::sim-engine/type))))
    (is (= [:a :b]
           (->> ((sut/sorter [(sut/compare-types [:a :b :c])])
                 [#:automaton-simulation-de.simulation-engine{:type :b
                                                              :date 2}
                  #:automaton-simulation-de.simulation-engine{:type :a
                                                              :date 1}])
                (mapv ::sim-engine/type))))))

(deftest data-to-fn-test
  (is (= nil (sut/data-to-fn [:non-existing-one])) "Invalid ordering returns `nil`.")
  (is (= 666
         ((sut/data-to-fn [::sim-engine/field ::sim-engine/product])
          #:automaton-simulation-de.simulation-engine{:type :product
                                                      :date 0}
          #:automaton-simulation-de.simulation-engine{:type :product
                                                      :date 1}))
      "Valid ordering works."))
