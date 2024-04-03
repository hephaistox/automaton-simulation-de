(ns automaton-simulation-de.middleware.request-test
  (:require
   #?(:clj [clojure.test :refer [deftest is testing]]
      :cljs [cljs.test :refer [deftest is testing] :include-macros true])
   [automaton-core.adapters.schema :as core-schema]
   [automaton-simulation-de.middleware.request :as sut]
   [automaton-simulation-de.scheduler.event :as sim-de-event]
   [automaton-simulation-de.scheduler.snapshot :as sim-de-snapshot]))

(deftest schema-test
  (testing "Test the schema"
    (is (nil? (core-schema/validate-humanize (sut/schema))))))

(deftest build-test
  (testing "Is the build request returning the appropriate schema"
    (is (nil? (core-schema/validate-data-humanize (sut/schema)
                                                  (sut/build (fn [_request]
                                                               {})
                                                             (sim-de-snapshot/build 10 10 10
                                                                                    {} []
                                                                                    (sim-de-event/make-events :a 1))
                                                             (sim-de-event/make-event :a 1)
                                                             []))))))
(deftest prepare-request-test
  (testing "No snapshot means no future events"
    (is (=
         (sut/build nil nil nil [{:cause ::sut/no-future-events}])
         (sut/prepare nil nil nil))))

  (testing "No event execution is accepted"
    (is (empty? (-> (sut/prepare nil
                                 (sim-de-snapshot/build 1 1 1 nil nil
                                                        (sim-de-event/make-events :a 1 :b 1))
                                 nil)
                    ::sut/stop))))
  (testing "Last event in the future implies the stop of the simulation"
    (is (= [{:cause ::sut/no-future-events}]
           (-> (sut/prepare nil
                            (sim-de-snapshot/build 1 1 1 nil nil
                                                   [])
                            nil)
               ::sut/stop)))))
