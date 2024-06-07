(ns automaton-simulation-de.request-test
  (:require
   #?(:clj [clojure.test :refer [deftest is testing]]
      :cljs [cljs.test :refer [deftest is testing] :include-macros true])
   [automaton-core.adapters.schema             :as core-schema]
   [automaton-simulation-de.ordering           :as sim-de-ordering]
   [automaton-simulation-de.request            :as sut]
   [automaton-simulation-de.scheduler.event    :as sim-de-event]
   [automaton-simulation-de.scheduler.snapshot :as sim-de-snapshot]))

(deftest schema-test
  (testing "Test the schema"
    (is (nil? (core-schema/validate-humanize sut/schema)))))

(deftest add-stopping-cause-test
  (testing "Adding a `stopping-cause` returns the request."
    (is (= {:request true
            ::sut/stopping-causes [{:stopping-cause true}]}
           (sut/add-stopping-cause {:request true} {:stopping-cause true}))))
  (testing "Adding `nil` returns the request."
    (is (= {:request true} (sut/add-stopping-cause {:request true} nil)))))

(deftest build-test
  (testing "Is the build request returning the appropriate schema."
    (is
     (nil?
      (core-schema/validate-data-humanize
       (core-schema/close-map-schema sut/schema)
       (sut/build
        (sim-de-event/make-event :a 1)
        (fn [_request] {})
        (sim-de-snapshot/build 10 10 10 {} [] (sim-de-event/make-events :a 1))
        (sim-de-ordering/data-to-fn [:field ::sim-de-event/date])))))))
