(ns automaton-simulation-de.impl.built-in-sd.no-future-events-test
  (:require
   #?(:clj [clojure.test :refer [deftest is testing]]
      :cljs [cljs.test :refer [deftest is testing] :include-macros true])
   [automaton-core.adapters.schema                            :as core-schema]
   [automaton-simulation-de.impl.built-in-sd.no-future-events :as sut]
   [automaton-simulation-de.impl.stopping.definition
    :as sim-de-sc-definition]
   [automaton-simulation-de.request                           :as
                                                              sim-de-request]
   [automaton-simulation-de.scheduler.event                   :as sim-de-event]
   [automaton-simulation-de.scheduler.snapshot                :as
                                                              sim-de-snapshot]))

(deftest stopping-definition-test
  (is (nil? (->> sut/stopping-definition
                 (core-schema/validate-data-humanize
                  sim-de-sc-definition/schema)))))

(deftest evaluates-test
  (is (nil? (core-schema/validate-data-humanize
             sim-de-request/schema
             (sut/evaluates (sim-de-request/build
                             (sim-de-event/make-event :a 1)
                             (constantly nil)
                             (sim-de-snapshot/build 1 1 1 {} [] [])
                             (constantly nil))
                            []))))
  (testing "If some `future-event` exists, return `nil` stopping criteria."
    (is (= {:request true}
           (sut/evaluates {:request true} (sim-de-event/make-events :a 1)))))
  (testing "If no `future-event` exists, returns the `stopping-cause`."
    (is (= :no-future-events
           (-> (sut/evaluates nil [])
               ::sim-de-request/stopping-causes
               first
               (get-in [:stopping-criteria :stopping-definition :id]))))))
