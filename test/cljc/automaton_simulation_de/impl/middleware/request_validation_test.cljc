(ns automaton-simulation-de.impl.middleware.request-validation-test
  (:require
   #?(:clj [clojure.test :refer [deftest is testing]]
      :cljs [cljs.test :refer [deftest is testing] :include-macros true])
   [automaton-core.adapters.schema                             :as core-schema]
   [automaton-simulation-de.impl.middleware.request-validation :as sut]
   [automaton-simulation-de.impl.stopping.cause
    :as sim-de-stopping-cause]
   [automaton-simulation-de.request                            :as
                                                               sim-de-request]
   [automaton-simulation-de.scheduler.event                    :as sim-de-event]
   [automaton-simulation-de.scheduler.snapshot
    :as sim-de-snapshot]))

(deftest evaluates-test
  (testing "Well form request is not modifying the request."
    (is (nil? (-> (sim-de-request/build (sim-de-event/make-event :a 1)
                                        (constantly {})
                                        (sim-de-snapshot/build
                                         1
                                         1
                                         1
                                         {}
                                         []
                                         (sim-de-event/make-events :a 1 :b 2))
                                        (constantly nil))
                  sut/evaluates)))
    (is (empty? (-> (sim-de-request/build (sim-de-event/make-event :a 1)
                                          (constantly {})
                                          (sim-de-snapshot/build
                                           1
                                           1
                                           1
                                           {}
                                           []
                                           (sim-de-event/make-events :a 1 :b 2))
                                          (constantly nil))
                    sut/evaluates
                    ::sim-de-request/stopping-causes))))
  (is (nil? (core-schema/validate-data-humanize sim-de-stopping-cause/schema
                                                (sut/evaluates nil))))
  (testing "Well form request is not modifying the request."
    (let [response (sim-de-request/build (sim-de-event/make-event :a 1)
                                         (constantly {})
                                         (sim-de-snapshot/build
                                          1
                                          1
                                          1
                                          {}
                                          []
                                          (sim-de-event/make-events :a 1 :b 2))
                                         (constantly true))]
      (is (nil? (sut/evaluates response))))
    (is (empty? (-> (sim-de-request/build (sim-de-event/make-event :a 1)
                                          (constantly {})
                                          (sim-de-snapshot/build
                                           1
                                           1
                                           1
                                           {}
                                           []
                                           (sim-de-event/make-events :a 1 :b 2))
                                          (constantly true))
                    sut/evaluates
                    ::sim-de-request/stopping-causes))))
  (is (nil? (core-schema/validate-data-humanize sim-de-stopping-cause/schema
                                                (sut/evaluates nil)))))
