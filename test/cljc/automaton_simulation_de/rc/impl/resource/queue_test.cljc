(ns automaton-simulation-de.rc.impl.resource.queue-test
  (:require
   [automaton-core.adapters.schema                            :as core-schema]
   #?(:clj [clojure.test :refer [deftest are is testing]]
      :cljs [cljs.test :refer [deftest are is testing] :include-macros true])
   [automaton-simulation-de.rc                                :as sim-de-rc]
   [automaton-simulation-de.rc.impl.preemption-policy.factory
    :as sim-de-rc-preemption-policy-factory]
   [automaton-simulation-de.rc.impl.resource.queue            :as sut]
   [automaton-simulation-de.rc.impl.unblocking-policy.factory
    :as sim-de-rc-unblocking-policy-factory]
   [automaton-simulation-de.scheduler.event                   :as
                                                              sim-de-event]))

(defn uncache
  "As testing the content of the cache is not necessary and cumbersome, this function removes it so we can focus on `queue` testing."
  [resource]
  (dissoc resource :automaton-simulation-de.rc/cache))

(defn add-cache
  "The cached functions are necessary to test this namespace. They are normally added by the defaulting of the resource.
  As we don't want to entangle the test with this defaulting, this function adds the required data only."
  [resource]
  (assoc resource
         :automaton-simulation-de.rc/cache
         {::sim-de-rc/unblocking-policy-fn
          sim-de-rc-unblocking-policy-factory/default-policy
          ::sim-de-rc/preemption-policy-fn
          sim-de-rc-preemption-policy-factory/default-policy}))

(defn- unqueue-event-cacheproof
  [resource available-capacity]
  (update (sut/unqueue-event (add-cache resource) available-capacity)
          1
          uncache))

(deftest schema-test
  (testing "Validate queue schema"
    (is (nil? (core-schema/validate-humanize (sut/schema))))))

(deftest queue-event-test
  (testing "Empty events are not added"
    (is (= {}
           (sut/queue-event nil 1 {})
           (sut/queue-event {} 1 {})
           (sut/queue-event {} 1 nil))))
  (testing "The first event is added in the empty queue"
    (is (= #::sim-de-rc{:queue [#::sim-de-rc{:seizing-event
                                             (sim-de-event/make-event :a 1)
                                             :consumed-quantity 13}]
                        :resource-name ::test}
           (sut/queue-event #::sim-de-rc{:resource-name ::test}
                            13
                            (sim-de-event/make-event :a 1)))))
  (testing "Further events are added"
    (is (= 3
           (-> (sut/queue-event #::sim-de-rc{:resource-name ::test
                                             :queue [{} {}]}
                                13
                                (sim-de-event/make-event :a 1))
               ::sim-de-rc/queue
               count))))
  (testing "Non stricty positive `consumed-quantity` are ignored"
    (are [x] (= 2
                (-> x
                    ::sim-de-rc/queue
                    count))
     (-> (sut/queue-event #::sim-de-rc{:resource-name ::test
                                       :queue [{} {}]}
                          0
                          (sim-de-event/make-event :a 1)))
     (-> (sut/queue-event #::sim-de-rc{:resource-name ::test
                                       :queue [{} {}]}
                          -1
                          (sim-de-event/make-event :a 1)))
     (-> (sut/queue-event #::sim-de-rc{:resource-name ::test
                                       :queue [{} {}]}
                          ""
                          (sim-de-event/make-event :a 1)))
     (-> (sut/queue-event #::sim-de-rc{:resource-name ::test
                                       :queue [{} {}]}
                          nil
                          (sim-de-event/make-event :a 1))))))

(deftest unqueue-event-test
  (testing "Non integer `available-capacity` is ok"
    (is (= [[] {::sim-de-rc/queue []}]
           (unqueue-event-cacheproof nil nil)
           (unqueue-event-cacheproof
            #::sim-de-rc{:queue [{::sim-de-rc/seizing-event
                                  (sim-de-event/make-events :a 1 :b 2)}]}
            nil))))
  (testing "Unqueue empty queue is ok"
    (is (= [[] #::sim-de-rc{:queue []}] (unqueue-event-cacheproof nil 1))))
  (testing
    "Unqueue a non positive integer of `available-capacity` in a non empty queue is removing one and only one event"
    (is (= [[] #::sim-de-rc{:queue [{:a 2}]}]
           (unqueue-event-cacheproof #::sim-de-rc{:queue [{:a 2}]} 0))))
  (testing "Unqueue takes the first event and leave next ones in the queue"
    (is (= [[#::sim-de-rc{:seizing-event (sim-de-event/make-event :a 1)}]
            #::sim-de-rc{:queue [#::sim-de-rc{:seizing-event
                                              (sim-de-event/make-event :b 2)}]}]
           (unqueue-event-cacheproof
            #::sim-de-rc{:queue [#::sim-de-rc{:seizing-event
                                              (sim-de-event/make-event :a 1)}
                                 #::sim-de-rc{:seizing-event
                                              (sim-de-event/make-event :b 2)}]}
            1)))
    (is (= [[#::sim-de-rc{:seizing-event (sim-de-event/make-event :a 1)
                          :consumed-quantity 1}]
            #::sim-de-rc{:queue [#::sim-de-rc{:seizing-event
                                              (sim-de-event/make-event :b 2)}]}]
           (unqueue-event-cacheproof
            #::sim-de-rc{:queue [#::sim-de-rc{:seizing-event
                                              (sim-de-event/make-event :a 1)
                                              :consumed-quantity 1}
                                 #::sim-de-rc{:seizing-event
                                              (sim-de-event/make-event :b 2)}]}
            1))))
  (testing "Unqueue can drop the last element"
    (is (= [[#::sim-de-rc{:seizing-event (sim-de-event/make-event :a 0)}]
            {::sim-de-rc/queue []}]
           (unqueue-event-cacheproof
            #::sim-de-rc{:queue [#::sim-de-rc{:seizing-event
                                              (sim-de-event/make-event :a 0)}]}
            1))))
  (testing
    "Thread queue and unqueue to check unqueueing is finding the first queued element first"
    (is (= [[#::sim-de-rc{:seizing-event {:a :b}
                          :consumed-quantity 1}]
            #::sim-de-rc{:queue []}]
           (-> {}
               (sut/queue-event 1 {:a :b})
               (unqueue-event-cacheproof 1))))
    (is (= [[#::sim-de-rc{:seizing-event {:a :b}
                          :consumed-quantity 17}]
            #::sim-de-rc{:queue [#::sim-de-rc{:seizing-event {:a :c}
                                              :consumed-quantity 19}
                                 #::sim-de-rc{:seizing-event {:d :b}
                                              :consumed-quantity 11}]}]
           (-> {}
               (sut/queue-event 17 {:a :b})
               (sut/queue-event 19 {:a :c})
               (sut/queue-event 11 {:d :b})
               (unqueue-event-cacheproof 17)))))
  (testing "unqueue more than the remaining capacity"
    (is (= [[#::sim-de-rc{:seizing-event {:a :b}
                          :consumed-quantity 1}
             #::sim-de-rc{:seizing-event {:d :b}
                          :consumed-quantity 2}]
            #::sim-de-rc{:queue []}]
           (-> {}
               (sut/queue-event 1 {:a :b})
               (sut/queue-event 2 {:d :b})
               (unqueue-event-cacheproof 5)))))
  (testing "unqueue less than the first capacity"
    (is (= [[]
            #::sim-de-rc{:queue [#::sim-de-rc{:seizing-event {:a :b}
                                              :consumed-quantity 10}
                                 #::sim-de-rc{:seizing-event {:d :b}
                                              :consumed-quantity 20}]}]
           (-> {}
               (sut/queue-event 10 {:a :b})
               (sut/queue-event 20 {:d :b})
               (unqueue-event-cacheproof 5)))))
  (testing "unqueue exactly the expected capacity of two events"
    (is (= [[#::sim-de-rc{:seizing-event {:a :b}
                          :consumed-quantity 1}
             #::sim-de-rc{:seizing-event {:d :b}
                          :consumed-quantity 2}]
            #::sim-de-rc{:queue []}]
           (-> {}
               (sut/queue-event 1 {:a :b})
               (sut/queue-event 2 {:d :b})
               (unqueue-event-cacheproof 3))))
    (is (= [[#::sim-de-rc{:seizing-event {:a :b}
                          :consumed-quantity 1}]
            #::sim-de-rc{:queue [#::sim-de-rc{:seizing-event {:d :b}
                                              :consumed-quantity 2}]}]
           (-> {}
               (sut/queue-event 1 {:a :b})
               (sut/queue-event 2 {:d :b})
               (unqueue-event-cacheproof 2))))
    (is (= [[#::sim-de-rc{:seizing-event {:a :b}
                          :consumed-quantity 1}
             #::sim-de-rc{:seizing-event {:d :b}
                          :consumed-quantity 2}]
            #::sim-de-rc{:queue []}]
           (-> {}
               (sut/queue-event 1 {:a :b})
               (sut/queue-event 2 {:d :b})
               (unqueue-event-cacheproof 4))))
    (is (= [[#::sim-de-rc{:seizing-event {:a :b}
                          :consumed-quantity 1}
             #::sim-de-rc{:seizing-event {:b :b}
                          :consumed-quantity 2}]
            #::sim-de-rc{:queue [#::sim-de-rc{:seizing-event {:c :b}
                                              :consumed-quantity 4}
                                 #::sim-de-rc{:seizing-event {:d :b}
                                              :consumed-quantity 2}]}]
           (-> {}
               (sut/queue-event 1 {:a :b})
               (sut/queue-event 2 {:b :b})
               (sut/queue-event 4 {:c :b})
               (sut/queue-event 2 {:d :b})
               (unqueue-event-cacheproof 4))))))
