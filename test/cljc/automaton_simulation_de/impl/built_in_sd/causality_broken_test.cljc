(ns automaton-simulation-de.impl.built-in-sd.causality-broken-test
  (:require
   #?(:clj [clojure.test :refer [deftest is testing]]
      :cljs [cljs.test :refer [deftest is testing] :include-macros true])
   [automaton-core.adapters.schema                            :as core-schema]
   [automaton-simulation-de.impl.built-in-sd.causality-broken :as sut]
   [automaton-simulation-de.impl.stopping.cause
    :as sim-de-stopping-cause]
   [automaton-simulation-de.impl.stopping.definition
    :as sim-de-sc-definition]
   [automaton-simulation-de.scheduler.event                   :as sim-de-event]
   [automaton-simulation-de.scheduler.snapshot                :as
                                                              sim-de-snapshot]))

(deftest stopping-definition-test
  (is (nil? (->> sut/stopping-definition
                 (core-schema/validate-data-humanize
                  sim-de-sc-definition/schema)))))

(def event-stub (sim-de-event/make-event :a 1))

(deftest evaluates-test
  (testing "Same date snapshot are accepted."
    (is (nil? (sut/evaluates #::sim-de-snapshot{:date 12}
                             #::sim-de-snapshot{:date 14}
                             event-stub))))
  (testing "Greater date snapshot are accepted."
    (is (nil? (sut/evaluates #::sim-de-snapshot{:date 12}
                             #::sim-de-snapshot{:date 12}
                             event-stub))))
  (testing
    "If next sanpshot' date is smaller than current one, an error is raised."
    (is (nil? (->> (sut/evaluates #::sim-de-snapshot{:date 15}
                                  #::sim-de-snapshot{:date 12}
                                  event-stub)
                   (core-schema/validate-data-humanize
                    sim-de-stopping-cause/schema))))))
