(ns automaton-simulation-de.impl.scheduler-test
  (:require
   #?(:clj [clojure.test :refer [deftest is testing]]
      :cljs [cljs.test :refer [deftest is testing] :include-macros true])
   [automaton-simulation-de.impl.model             :as sim-de-model]
   [automaton-simulation-de.impl.registry          :as sim-de-registry]
   [automaton-simulation-de.impl.scheduler         :as sut]
   [automaton-simulation-de.ordering               :as sim-de-ordering]
   [automaton-simulation-de.request                :as sim-de-request]
   [automaton-simulation-de.response               :as sim-de-response]
   [automaton-simulation-de.scheduler.event        :as sim-de-event]
   [automaton-simulation-de.scheduler.event-return :as sim-de-event-return]
   [automaton-simulation-de.scheduler.snapshot     :as sim-de-snapshot]))

(defn- first-stopping-definition-id
  [response]
  (-> response
      ::sim-de-response/stopping-causes
      first
      :stopping-criteria
      :stopping-definition
      :id))

(defn- snapshot-id
  [response]
  (-> response
      ::sim-de-response/snapshot
      ::sim-de-snapshot/id))

(defn- snapshot-iteration
  [response]
  (-> response
      ::sim-de-response/snapshot
      ::sim-de-snapshot/iteration))

(defn- state
  [response]
  (get-in response [::sim-de-response/snapshot ::sim-de-snapshot/state]))

(defn- future-events
  [response]
  (get-in response
          [::sim-de-response/snapshot ::sim-de-snapshot/future-events]))

(defn- latest-past-event
  [response]
  (-> response
      (get-in [::sim-de-response/snapshot ::sim-de-snapshot/past-events])
      last))

(defn- snapshot-date
  [response]
  (-> response
      ::sim-de-response/snapshot
      ::sim-de-snapshot/date))

(defn events-stub [] (sim-de-event/make-events :a 13 :b 14 :d 15))

(def ^:private snapshot-stub
  (sim-de-snapshot/build 2 2 2 {:foo :bar} [] (sim-de-event/make-events :a 30)))

(def ^:private request-stub
  (sim-de-request/build nil
                        nil
                        snapshot-stub
                        (sim-de-ordering/sorter [(sim-de-ordering/compare-field
                                                  ::sim-de-event/date)])))

(deftest handler-test
  (testing
    "When a request has raised a `stopping-cause`, it is passed to the response and the `snapshot` is not modified."
    (is (= (sim-de-response/build [{:stopping-criteria :test-stopping}]
                                  snapshot-stub)
           (-> request-stub
               (assoc ::sim-de-request/event-execution (constantly {}))
               (sim-de-request/add-stopping-cause {:stopping-criteria
                                                   :test-stopping})
               sut/handler))))
  (testing
    "If no valid `event-execution` is detected, the `execution-not-found` `stopping-cause` is added, `bucket` is not changed, but iteration is incremented."
    (is (= [:execution-not-found 30 3]
           ((juxt first-stopping-definition-id snapshot-date snapshot-id)
            (sut/handler request-stub)))))
  (testing "For a valid request.\n"
    (testing
      "Empty `future-events` creates a new `snapshot-id`, doesn't change the snapshot date and creates an `execution-not-found` as it is `nil`."
      (is (= [2 :execution-not-found 3]
             ((juxt snapshot-date first-stopping-definition-id snapshot-id)
              (-> request-stub
                  (assoc-in [::sim-de-request/snapshot
                             ::sim-de-snapshot/future-events]
                            [])
                  sut/handler)))))
    (testing
      "When valid, the first event in the future list is turned into a `past-event`, it creates no `stopping-cause`"
      (is (= [nil (events-stub) (sim-de-event/make-event :a 30) {:foo3 :bar3}]
             ((juxt first-stopping-definition-id
                    future-events
                    latest-past-event
                    state)
              (-> request-stub
                  (assoc ::sim-de-request/event-execution
                         (constantly (sim-de-event-return/build
                                      {:foo3 :bar3}
                                      (shuffle (events-stub)))))
                  sut/handler)))))
    (testing
      "When an `handler` is throwing an exception, it creates a `failed-event-execution` `stopping-cause`, the `event-execution` is skipped, but a new `snapshot` is created, with its incremented iteration number and with bucket of failed event."
      (is (= [:failed-event-execution 3 30 3]
             ((juxt first-stopping-definition-id
                    snapshot-id
                    snapshot-date
                    snapshot-iteration)
              (-> request-stub
                  (assoc ::sim-de-request/event-execution
                         #(throw (ex-info "Arg" {})))
                  sut/handler)))))
    (testing
      "Snapshot bucket is `100`, but an event happened at `13`, so in the past and causality rule is broken, the `stopping-cause`'s `stopping-criteria` is added. Note `future-events` and `state` are replaced with values returned from event execution."
      (is (= [:causality-broken (events-stub) {:foo3 :bar3}]
             ((juxt first-stopping-definition-id future-events state)
              (-> request-stub
                  (assoc-in [::sim-de-request/snapshot ::sim-de-snapshot/date]
                            100)
                  (assoc ::sim-de-request/event-execution
                         (constantly (sim-de-event-return/build
                                      {:foo3 :bar3}
                                      (shuffle (events-stub)))))
                  sut/handler)))))))

(defn event-registry-stub
  [added-future-events]
  {:a (fn [_ state future-events]
        (sim-de-event-return/build (assoc state :sc :sd)
                                   (concat future-events
                                           added-future-events)))})

(defn registry-stub
  [added-future-events]
  {:event (event-registry-stub added-future-events)
   :middleware {}
   :stopping {}
   :ordering {}})

(defn initial-snapshot
  [future-events]
  (sim-de-snapshot/build 1 1 1 {:sa :sb} [] future-events))

(deftest scheduler-loop-test
  (testing "Nil values are ok, it implies no future-event is detected."
    (is (= [:no-future-events]
           (->> (sut/scheduler-loop nil nil sut/handler nil [])
                ::sim-de-response/stopping-causes
                (mapv (comp :id :stopping-definition :stopping-criteria))))))
  (testing
    "First event is properly executed, state and future events are up to date, iteration, date and id are increased, state updated, first event is gone in the past."
    (is (= (sim-de-response/build
            []
            (sim-de-snapshot/build
             2
             2
             10
             {:sa :sb
              :sc :sd}
             (sim-de-event/make-events :a 10)
             (sim-de-event/make-events :b 12 :a 13 :b 14)))
           (sut/scheduler-loop
            (event-registry-stub (sim-de-event/make-events :a 13 :b 14))
            (sim-de-ordering/sorter nil)
            sut/handler
            (initial-snapshot (sim-de-event/make-events :a 10 :b 12))
            []))))
  (testing
    "The last `event` should be executed properly, it happens when `future-events` has only one event, and none is added by the `event-execution`."
    (is (= (sim-de-response/build []
                                  (sim-de-snapshot/build
                                   2
                                   2
                                   10
                                   {:sa :sb
                                    :sc :sd}
                                   (sim-de-event/make-events :a 10)
                                   []))
           (sut/scheduler-loop (event-registry-stub [])
                               (sim-de-ordering/sorter nil)
                               sut/handler
                               (initial-snapshot (sim-de-event/make-events :a
                                                                           10))
                               []))))
  (testing
    "When `future-events` is empty`, the `no-future-events` `stopping-cause` is added, the same snapshot is returned, without changing anything."
    (is (= [:no-future-events 1 1]
           ((juxt first-stopping-definition-id snapshot-id snapshot-date)
            (sut/scheduler-loop (event-registry-stub [])
                                (sim-de-ordering/sorter nil)
                                sut/handler
                                (initial-snapshot [])
                                []))))))

(deftest scheduler-test
  (testing
    "Executing no event is ok, it is returning the same snapshot and stops with `no-future-events`."
    (is (= [1 1 :no-future-events {:sa :sb}]
           ((juxt snapshot-id snapshot-date first-stopping-definition-id state)
            (sut/scheduler (sim-de-model/build {:initial-event-type :IN}
                                               (sim-de-registry/build))
                           []
                           []
                           (initial-snapshot []))))))
  (testing
    "Executing one only event is ok, it is creating only one `snapshot`, is at the `bucket` of the executed event and has updated the `state`."
    (is (= [2
            4
            :no-future-events
            {:sa :sb
             :sc :sd}]
           ((juxt snapshot-id snapshot-date first-stopping-definition-id state)
            (sut/scheduler
             (sim-de-model/build {:initial-event-type :IN} (registry-stub []))
             []
             []
             (initial-snapshot (sim-de-event/make-events :a 4)))))))
  (testing "Executing 3 events is ok, it is creating 3 snapshots."
    (is (= [4
            50
            :no-future-events
            {:sa :sb
             :sc :sd}]
           ((juxt snapshot-id snapshot-date first-stopping-definition-id state)
            (sut/scheduler (sim-de-model/build {:initial-event-type :IN}
                                               (registry-stub []))
                           []
                           []
                           (initial-snapshot
                            (sim-de-event/make-events :a 40 :a 40 :a 50))))))))
