(ns automaton-simulation-de.impl.stopping-definition.iteration-nth-test
  (:require
   #?(:clj [clojure.test :refer [deftest is testing]]
      :cljs [cljs.test :refer [deftest is testing] :include-macros true])
   [automaton-core.adapters.schema                                 :as
                                                                   core-schema]
   [automaton-simulation-de.impl.stopping-definition.iteration-nth :as sut]
   [automaton-simulation-de.impl.stopping.definition
    :as sim-de-sc-definition]
   [automaton-simulation-de.scheduler.snapshot
    :as sim-de-snapshot]))

(deftest stop-nth-test
  (testing
    "Stops when the iteration number of the snpahost is greater or equal to the parameter."
    (is (sut/stop-nth {::sim-de-snapshot/iteration 12} {:n 10}))
    (is (sut/stop-nth {::sim-de-snapshot/iteration 12} {:n 12})))
  (testing
    "Doesn't stop when the iteration number of the snpahost is greater or equal to the parameter."
    (is (not (:stop? (sut/stop-nth {::sim-de-snapshot/iteration 2} {:n 10})))))
  (testing "Snapshot with no iteration number is default to 0"
    (is (not (:stop? (sut/stop-nth {} {:n 12}))))
    (is (:stop? (sut/stop-nth {} {:n 0})))
    (testing
      "No parameter is defaulted to `-1`, stopping whatever is happening."
      (is (sut/stop-nth {} {})))))

(deftest stopping-definition-test
  (is (nil? (->> (sut/stopping-definition)
                 (core-schema/validate-data-humanize
                  sim-de-sc-definition/schema)))))
