(ns automaton-simulation-de.simulation-engine.impl.scheduler-test
  (:require
   #?(:clj [clojure.test :refer [deftest is testing]]
      :cljs [cljs.test :refer [deftest is testing] :include-macros true])
   [automaton-simulation-de.simulation-engine                :as-alias sim-engine]
   [automaton-simulation-de.simulation-engine.impl.model     :as sim-de-model]
   [automaton-simulation-de.simulation-engine.impl.registry  :as sim-de-registry]
   [automaton-simulation-de.simulation-engine.impl.scheduler :as sut]
   [automaton-simulation-de.simulation-engine.ordering       :as sim-de-ordering]
   [automaton-simulation-de.simulation-engine.request        :as sim-de-request]))

(defn- first-stopping-definition-id
  [response]
  (-> response
      ::sim-engine/stopping-causes
      first
      ::sim-engine/stopping-criteria
      ::sim-engine/stopping-definition
      ::sim-engine/id))

(defn- snapshot-id
  [response]
  (-> response
      ::sim-engine/snapshot
      ::sim-engine/id))

(defn- snapshot-iteration
  [response]
  (-> response
      ::sim-engine/snapshot
      ::sim-engine/iteration))

(defn- state [response] (get-in response [::sim-engine/snapshot ::sim-engine/state]))

(defn- future-events
  [response]
  (get-in response [::sim-engine/snapshot ::sim-engine/future-events]))

(defn- latest-past-event
  [response]
  (-> response
      (get-in [::sim-engine/snapshot ::sim-engine/past-events])
      last))

(defn- snapshot-date
  [response]
  (-> response
      ::sim-engine/snapshot
      ::sim-engine/date))

(def events-stub
  [#:automaton-simulation-de.simulation-engine{:type :a
                                               :date 13}
   #:automaton-simulation-de.simulation-engine{:type :b
                                               :date 14}
   #:automaton-simulation-de.simulation-engine{:type :d
                                               :date 15}])

(def ^:private snapshot-stub
  #:automaton-simulation-de.simulation-engine{:id 2
                                              :iteration 2
                                              :date 2
                                              :state {:foo :bar}
                                              :past-events []
                                              :future-events
                                              [#:automaton-simulation-de.simulation-engine{:type :a
                                                                                           :date
                                                                                           30}]})

(def ^:private request-stub
  #:automaton-simulation-de.simulation-engine{:current-event nil
                                              :event-execution nil
                                              :snapshot snapshot-stub
                                              :stopping-causes []
                                              :sorting (sim-de-ordering/sorter
                                                        [(sim-de-ordering/compare-field
                                                          ::sim-engine/date)])})

(deftest handler-test
  (is
   (=
    #:automaton-simulation-de.simulation-engine{:stopping-causes
                                                [#:automaton-simulation-de.simulation-engine{:stopping-criteria
                                                                                             :test-stopping}]
                                                :snapshot snapshot-stub}
    (-> request-stub
        (assoc ::sim-engine/event-execution (constantly {}))
        (sim-de-request/add-stopping-cause
         #:automaton-simulation-de.simulation-engine{:stopping-criteria :test-stopping})
        sut/handler))
   "When a request has raised a `stopping-cause`, it is passed to the response and the `snapshot` is not modified.")
  (is
   (= [::sim-engine/execution-not-found 30 3]
      ((juxt first-stopping-definition-id snapshot-date snapshot-id) (sut/handler request-stub)))
   "If no valid `event-execution` is detected, the `execution-not-found` `stopping-cause` is added, `bucket` is not changed, but iteration is incremented.")
  (testing "For a valid request.\n"
    (is
     (= [2 ::sim-engine/execution-not-found 3]
        ((juxt snapshot-date first-stopping-definition-id snapshot-id)
         (-> request-stub
             (assoc-in [::sim-engine/snapshot ::sim-engine/future-events] [])
             sut/handler)))
     "Empty `future-events` creates a new `snapshot-id`, doesn't change the snapshot date and creates an `execution-not-found` as it is `nil`.")
    (is
     (= [nil
         events-stub
         #:automaton-simulation-de.simulation-engine{:type :a
                                                     :date 30}
         {:foo3 :bar3}]
        ((juxt first-stopping-definition-id future-events latest-past-event state)
         (-> request-stub
             (assoc ::sim-engine/event-execution
                    (constantly #:automaton-simulation-de.simulation-engine{:state {:foo3 :bar3}
                                                                            :future-events
                                                                            (shuffle events-stub)}))
             sut/handler)))
     "When valid, the first event in the future list is turned into a `past-event`, it creates no `stopping-cause`")
    (is
     (= [::sim-engine/failed-event-execution 3 30 3]
        ((juxt first-stopping-definition-id snapshot-id snapshot-date snapshot-iteration)
         (-> request-stub
             (assoc ::sim-engine/event-execution #(throw (ex-info "Arg" {})))
             sut/handler)))
     "When an `handler` is throwing an exception, it creates a `failed-event-execution` `stopping-cause`, the `event-execution` is skipped, but a new `snapshot` is created, with its incremented iteration number and with bucket of failed event.")
    (is
     (= [::sim-engine/causality-broken events-stub {:foo3 :bar3}]
        ((juxt first-stopping-definition-id future-events state)
         (-> request-stub
             (assoc-in [::sim-engine/snapshot ::sim-engine/date] 100)
             (assoc ::sim-engine/event-execution
                    (constantly #:automaton-simulation-de.simulation-engine{:state {:foo3 :bar3}
                                                                            :future-events
                                                                            (shuffle events-stub)}))
             sut/handler)))
     "Snapshot bucket is `100`, but an event happened at `13`, so in the past and causality rule is broken, the `stopping-cause`'s `stopping-criteria` is added. Note `future-events` and `state` are replaced with values returned from event execution.")))

(defn event-registry-stub
  [added-future-events]
  {:a (fn [_ state future-events]
        #:automaton-simulation-de.simulation-engine{:state (assoc state :sc :sd)
                                                    :future-events (concat future-events
                                                                           added-future-events)})})

(defn registry-stub
  [added-future-events]
  #:automaton-simulation-de.simulation-engine{:event (event-registry-stub added-future-events)
                                              :middleware {}
                                              :stopping {}
                                              :ordering {}})

(defn initial-snapshot
  [future-events]
  #:automaton-simulation-de.simulation-engine{:id 1
                                              :date 1
                                              :iteration 1
                                              :state {:sa :sb}
                                              :past-events []
                                              :future-events future-events})

(deftest scheduler-loop-test
  (is (= [::sim-engine/no-future-events]
         (->> (sut/scheduler-loop nil nil sut/handler nil [])
              ::sim-engine/stopping-causes
              (mapv (comp ::sim-engine/id
                          ::sim-engine/stopping-definition
                          ::sim-engine/stopping-criteria))))
      "Nil values are ok, it implies no future-event is detected.")
  (is
   (=
    #:automaton-simulation-de.simulation-engine{:stopping-causes []
                                                :snapshot
                                                #:automaton-simulation-de.simulation-engine{:id 2
                                                                                            :iteration
                                                                                            2
                                                                                            :date 10
                                                                                            :state
                                                                                            {:sa :sb
                                                                                             :sc
                                                                                             :sd}
                                                                                            :past-events
                                                                                            [#:automaton-simulation-de.simulation-engine{:type
                                                                                                                                         :a
                                                                                                                                         :date
                                                                                                                                         10}]
                                                                                            :future-events
                                                                                            [#:automaton-simulation-de.simulation-engine{:type
                                                                                                                                         :b
                                                                                                                                         :date
                                                                                                                                         12}
                                                                                             #:automaton-simulation-de.simulation-engine{:type
                                                                                                                                         :a
                                                                                                                                         :date
                                                                                                                                         13}
                                                                                             #:automaton-simulation-de.simulation-engine{:type
                                                                                                                                         :b
                                                                                                                                         :date
                                                                                                                                         14}]}}
    (sut/scheduler-loop (event-registry-stub [#:automaton-simulation-de.simulation-engine{:type :a
                                                                                          :date 13}
                                              #:automaton-simulation-de.simulation-engine{:type :b
                                                                                          :date
                                                                                          14}])
                        (sim-de-ordering/sorter nil)
                        sut/handler
                        (initial-snapshot [#:automaton-simulation-de.simulation-engine{:type :a
                                                                                       :date 10}
                                           #:automaton-simulation-de.simulation-engine{:type :b
                                                                                       :date 12}])
                        []))
   "First event is properly executed, state and future events are up to date, iteration, date and id are increased, state updated, first event is gone in the past.")
  (is
   (=
    #:automaton-simulation-de.simulation-engine{:stopping-causes []
                                                :snapshot
                                                #:automaton-simulation-de.simulation-engine{:id 2
                                                                                            :iteration
                                                                                            2
                                                                                            :date 10
                                                                                            :state
                                                                                            {:sa :sb
                                                                                             :sc
                                                                                             :sd}
                                                                                            :past-events
                                                                                            [#:automaton-simulation-de.simulation-engine{:type
                                                                                                                                         :a
                                                                                                                                         :date
                                                                                                                                         10}]
                                                                                            :future-events
                                                                                            []}}
    (sut/scheduler-loop (event-registry-stub [])
                        (sim-de-ordering/sorter nil)
                        sut/handler
                        (initial-snapshot [#:automaton-simulation-de.simulation-engine{:type :a
                                                                                       :date 10}])
                        []))
   "The last `event` should be executed properly, it happens when `future-events` has only one event, and none is added by the `event-execution`.")
  (is
   (= [::sim-engine/no-future-events 1 1]
      ((juxt first-stopping-definition-id snapshot-id snapshot-date)
       (sut/scheduler-loop (event-registry-stub [])
                           (sim-de-ordering/sorter nil)
                           sut/handler
                           (initial-snapshot [])
                           [])))
   "When `future-events` is empty`, the `no-future-events` `stopping-cause` is added, the same snapshot is returned, without changing anything."))

(deftest scheduler-test
  (is
   (= [1 1 ::sim-engine/no-future-events {:sa :sb}]
      ((juxt snapshot-id snapshot-date first-stopping-definition-id state)
       (sut/scheduler (sim-de-model/build {} (sim-de-registry/build)) [] [] (initial-snapshot []))))
   "Executing no event is ok, it is returning the same snapshot and stops with `no-future-events`.")
  (is
   (= [2
       4
       ::sim-engine/no-future-events
       {:sa :sb
        :sc :sd}]
      ((juxt snapshot-id snapshot-date first-stopping-definition-id state)
       (sut/scheduler (sim-de-model/build {} (registry-stub []))
                      []
                      []
                      (initial-snapshot [#:automaton-simulation-de.simulation-engine{:type :a
                                                                                     :date 4}]))))
   "Executing one only event is ok, it is creating only one `snapshot`, is at the `bucket` of the executed event and has updated the `state`.")
  (is (= [4
          50
          ::sim-engine/no-future-events
          {:sa :sb
           :sc :sd}]
         ((juxt snapshot-id snapshot-date first-stopping-definition-id state)
          (sut/scheduler (sim-de-model/build {} (registry-stub []))
                         []
                         []
                         (initial-snapshot [#:automaton-simulation-de.simulation-engine{:type :a
                                                                                        :date 40}
                                            #:automaton-simulation-de.simulation-engine{:type :a
                                                                                        :date 40}
                                            #:automaton-simulation-de.simulation-engine{:type :a
                                                                                        :date
                                                                                        50}]))))
      "Executing 3 events is ok, it is creating 3 snapshots."))
