(ns automaton-simulation-de.simulation-engine.event-test
  (:require
   [automaton-core.adapters.schema                  :as core-schema]
   #?(:clj [clojure.test :refer [deftest is testing]]
      :cljs [cljs.test :refer [deftest is testing] :include-macros true])
   [automaton-simulation-de.simulation-engine       :as-alias sim-engine]
   [automaton-simulation-de.simulation-engine.event :as sut]))

(deftest schema-test
  (testing "Test schema of event" (is (= nil (core-schema/validate-humanize sut/schema))))
  (testing "Test valid events"
    (is (= nil
           (core-schema/validate-data-humanize sut/schema
                                               #:automaton-simulation-de.simulation-engine{:type :a
                                                                                           :date
                                                                                           12})))))

(deftest postpone-events-test
  (testing "Empty events are ok" (is (empty? (sut/postpone-events nil nil nil))))
  (testing "Example"
    (is
     (= [#:automaton-simulation-de.simulation-engine{:type :a
                                                     :date 1}
         #:automaton-simulation-de.simulation-engine{:type :b
                                                     :date 666}
         #:automaton-simulation-de.simulation-engine{:type :a
                                                     :date 3}
         #:automaton-simulation-de.simulation-engine{:type :c
                                                     :date 666}]
        (sut/postpone-events [#:automaton-simulation-de.simulation-engine{:type :a
                                                                          :date 1}
                              #:automaton-simulation-de.simulation-engine{:type :b
                                                                          :date 2}
                              #:automaton-simulation-de.simulation-engine{:type :a
                                                                          :date 3}
                              #:automaton-simulation-de.simulation-engine{:type :c
                                                                          :date 10}]
                             (comp even? ::sim-engine/date)
                             666))))
  (testing "None updated"
    (is
     (= [#:automaton-simulation-de.simulation-engine{:type :a
                                                     :date 1}
         #:automaton-simulation-de.simulation-engine{:type :b
                                                     :date 5}
         #:automaton-simulation-de.simulation-engine{:type :a
                                                     :date 3}
         #:automaton-simulation-de.simulation-engine{:type :c
                                                     :date 11}]
        (sut/postpone-events [#:automaton-simulation-de.simulation-engine{:type :a
                                                                          :date 1}
                              #:automaton-simulation-de.simulation-engine{:type :b
                                                                          :date 5}
                              #:automaton-simulation-de.simulation-engine{:type :a
                                                                          :date 3}
                              #:automaton-simulation-de.simulation-engine{:type :c
                                                                          :date 11}]
                             (comp even? ::sim-engine/date)
                             666)))))
