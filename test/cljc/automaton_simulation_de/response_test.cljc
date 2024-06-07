(ns automaton-simulation-de.response-test
  (:require
   #?(:clj [clojure.test :refer [deftest is testing]]
      :cljs [cljs.test :refer [deftest is testing] :include-macros true])
   [automaton-core.adapters.schema             :as core-schema]
   [automaton-simulation-de.response           :as sut]
   [automaton-simulation-de.scheduler.event    :as sim-de-event]
   [automaton-simulation-de.scheduler.snapshot :as sim-de-snapshot]))

(def event-stub (sim-de-event/make-event :a 1))

(deftest schema-test
  (testing "Test schema"
    (is (nil? (core-schema/validate-humanize sut/schema)))))

(deftest build-test
  (testing "Build response complies the schema."
    (is (->>
          (sut/build
           []
           (sim-de-snapshot/build 1 1 1 {} [] (sim-de-event/make-events :a 1)))
          (core-schema/validate-data-humanize (core-schema/close-map-schema
                                               sut/schema))
          nil?))))

(deftest add-stopping-cause-test
  (is (= #::sut{:stopping-causes [{:a :b}]}
         (sut/add-stopping-cause {::sut/stopping-causes []} {:a :b}))))

(deftest consume-first-event-test
  (testing
    "No future events implies no modification of date, but the incrementation of iteration and id."
    (is (= (sim-de-snapshot/build 4 4 10 {} [] [])
           (-> (sut/build [] (sim-de-snapshot/build 3 3 10 {} [] []))
               (sut/consume-first-event event-stub)
               ::sut/snapshot))))
  (testing
    "If the future event is happening in the past, then it breaks causality, the `date` is unchanged."
    (is
     (= :causality-broken
        (->
          (sut/build
           []
           (sim-de-snapshot/build 3 3 10 {} [] (sim-de-event/make-events :a 1)))
          (sut/consume-first-event event-stub)
          ::sut/stopping-causes
          first
          :stopping-criteria
          :stopping-definition
          :id)))
    (is
     (= 10
        (->
          (sut/build
           []
           (sim-de-snapshot/build 3 3 10 {} [] (sim-de-event/make-events :a 1)))
          (sut/consume-first-event event-stub)
          ::sut/snapshot
          ::sim-de-snapshot/date))))
  (testing
    "A future event at the same date than the current snapshot or later on is possible"
    (is
     (empty?
      (->
        (sut/build
         []
         (sim-de-snapshot/build 3 3 10 {} [] (sim-de-event/make-events :a 13)))
        (sut/consume-first-event event-stub)
        ::sut/stopping-causes)))
    (is
     (empty?
      (->
        (sut/build
         []
         (sim-de-snapshot/build 3 3 10 {} [] (sim-de-event/make-events :a 10)))
        (sut/consume-first-event event-stub)
        ::sut/stopping-causes)))))

(deftest add-current-event-to-stopping-causes-test
  (let [evt (sim-de-event/make-event :e 10)]
    (is (= (sut/build [{:a :b
                        :current-event evt}
                       {:c :d
                        :current-event evt}]
                      nil)
           (sut/add-current-event-to-stopping-causes
            (sut/build [{:a :b} {:c :d}] nil)
            (sim-de-event/make-event :e 10))))))
