(ns automaton-simulation-de.rc.impl.resource.queue-test
  (:require
   #?(:clj [clojure.test :refer [are deftest is testing]]
      :cljs [cljs.test :refer [are deftest is testing] :include-macros true])
   [automaton-core.adapters.schema                            :as core-schema]
   [automaton-simulation-de.rc                                :as sim-rc]
   [automaton-simulation-de.rc.impl.preemption-policy.factory :as sim-rc-preemption-policy-factory]
   [automaton-simulation-de.rc.impl.resource.queue            :as sut]
   [automaton-simulation-de.rc.impl.unblocking-policy.factory :as sim-rc-unblocking-policy-factory]
   [automaton-simulation-de.simulation-engine                 :as-alias sim-engine]))

(defn uncache
  "As testing the content of the cache is not necessary and cumbersome, this function removes it so we can focus on `queue` testing."
  [resource]
  (dissoc resource ::sim-rc/cache))

(defn add-cache
  "The cached functions are necessary to test this namespace. They are normally added by the defaulting of the resource.
  As we don't want to entangle the test with this defaulting, this function adds the required data only."
  [resource]
  (assoc resource
         ::sim-rc/cache
         {::sim-rc/unblocking-policy-fn sim-rc-unblocking-policy-factory/default-policy
          ::sim-rc/preemption-policy-fn sim-rc-preemption-policy-factory/default-policy}))

(defn- unqueue-event-cacheproof
  [resource available-capacity]
  (update (sut/unqueue-event (add-cache resource) available-capacity) 1 uncache))

(deftest schema-test
  (testing "Validate queue schema" (is (nil? (core-schema/validate-humanize sut/schema)))))

(deftest queue-event-test
  (testing "Empty events are not added"
    (is (= {} (sut/queue-event nil 1 {}) (sut/queue-event {} 1 {}) (sut/queue-event {} 1 nil))))
  (testing "The first event is added in the empty queue"
    (is
     (=
      #:automaton-simulation-de.rc{:queue
                                   [#:automaton-simulation-de.rc{:seizing-event
                                                                 #:automaton-simulation-de.simulation-engine{:type
                                                                                                             :a
                                                                                                             :date
                                                                                                             1}
                                                                 :consumed-quantity 13}]
                                   :resource-name ::test}
      (sut/queue-event #:automaton-simulation-de.rc{:resource-name ::test}
                       13
                       #:automaton-simulation-de.simulation-engine{:type :a
                                                                   :date 1}))))
  (testing "Further events are added"
    (is (= 3
           (-> (sut/queue-event #:automaton-simulation-de.rc{:resource-name ::test
                                                             :queue [{} {}]}
                                13
                                #:automaton-simulation-de.simulation-engine{:type :a
                                                                            :date 1})
               ::sim-rc/queue
               count))))
  (testing "Non stricty positive `consumed-quantity` are ignored"
    (are [x] (= 2
                (-> x
                    ::sim-rc/queue
                    count))
     (-> (sut/queue-event #:automaton-simulation-de.rc{:resource-name ::test
                                                       :queue [{} {}]}
                          0
                          #:automaton-simulation-de.simulation-engine{:type :a
                                                                      :date 1}))
     (-> (sut/queue-event #:automaton-simulation-de.rc{:resource-name ::test
                                                       :queue [{} {}]}
                          -1
                          #:automaton-simulation-de.simulation-engine{:type :a
                                                                      :date 1}))
     (-> (sut/queue-event #:automaton-simulation-de.rc{:resource-name ::test
                                                       :queue [{} {}]}
                          ""
                          #:automaton-simulation-de.simulation-engine{:type :a
                                                                      :date 1}))
     (-> (sut/queue-event #:automaton-simulation-de.rc{:resource-name ::test
                                                       :queue [{} {}]}
                          nil
                          #:automaton-simulation-de.simulation-engine{:type :a
                                                                      :date 1})))))

(deftest unqueue-event-test
  (testing "Non integer `available-capacity` is ok"
    (is (= [[] {::sim-rc/queue []}]
           (unqueue-event-cacheproof nil nil)
           (unqueue-event-cacheproof
            #:automaton-simulation-de.rc{:queue
                                         [{::sim-rc/seizing-event
                                           [#:automaton-simulation-de.simulation-engine{:type :a
                                                                                        :date 1}
                                            #:automaton-simulation-de.simulation-engine{:type :b
                                                                                        :date 2}]}]}
            nil))))
  (testing "Unqueue empty queue is ok"
    (is (= [[] #:automaton-simulation-de.rc{:queue []}] (unqueue-event-cacheproof nil 1))))
  (testing
    "Unqueue a non positive integer of `available-capacity` in a non empty queue is removing one and only one event"
    (is (= [[] #:automaton-simulation-de.rc{:queue [{:a 2}]}]
           (unqueue-event-cacheproof #:automaton-simulation-de.rc{:queue [{:a 2}]} 0))))
  (testing "Unqueue takes the first event and leave next ones in the queue"
    (is
     (=
      [[#:automaton-simulation-de.rc{:seizing-event
                                     #:automaton-simulation-de.simulation-engine{:type :a
                                                                                 :date 1}}]
       #:automaton-simulation-de.rc{:queue
                                    [#:automaton-simulation-de.rc{:seizing-event
                                                                  #:automaton-simulation-de.simulation-engine{:type
                                                                                                              :b
                                                                                                              :date
                                                                                                              2}}]}]
      (unqueue-event-cacheproof
       #:automaton-simulation-de.rc{:queue
                                    [#:automaton-simulation-de.rc{:seizing-event
                                                                  #:automaton-simulation-de.simulation-engine{:type
                                                                                                              :a
                                                                                                              :date
                                                                                                              1}}
                                     #:automaton-simulation-de.rc{:seizing-event
                                                                  #:automaton-simulation-de.simulation-engine{:type
                                                                                                              :b
                                                                                                              :date
                                                                                                              2}}]}
       1)))
    (is
     (=
      [[#:automaton-simulation-de.rc{:seizing-event
                                     #:automaton-simulation-de.simulation-engine{:type :a
                                                                                 :date 1}
                                     :consumed-quantity 1}]
       #:automaton-simulation-de.rc{:queue
                                    [#:automaton-simulation-de.rc{:seizing-event
                                                                  #:automaton-simulation-de.simulation-engine{:type
                                                                                                              :b
                                                                                                              :date
                                                                                                              2}}]}]
      (unqueue-event-cacheproof
       #:automaton-simulation-de.rc{:queue
                                    [#:automaton-simulation-de.rc{:seizing-event
                                                                  #:automaton-simulation-de.simulation-engine{:type
                                                                                                              :a
                                                                                                              :date
                                                                                                              1}
                                                                  :consumed-quantity 1}
                                     #:automaton-simulation-de.rc{:seizing-event
                                                                  #:automaton-simulation-de.simulation-engine{:type
                                                                                                              :b
                                                                                                              :date
                                                                                                              2}}]}
       1))))
  (testing "Unqueue can drop the last element"
    (is
     (=
      [[#:automaton-simulation-de.rc{:seizing-event
                                     #:automaton-simulation-de.simulation-engine{:type :a
                                                                                 :date 0}}]
       {::sim-rc/queue []}]
      (unqueue-event-cacheproof
       #:automaton-simulation-de.rc{:queue
                                    [#:automaton-simulation-de.rc{:seizing-event
                                                                  #:automaton-simulation-de.simulation-engine{:type
                                                                                                              :a
                                                                                                              :date
                                                                                                              0}}]}
       1))))
  (testing "Thread queue and unqueue to check unqueueing is finding the first queued element first"
    (is (= [[#:automaton-simulation-de.rc{:seizing-event {:a :b}
                                          :consumed-quantity 1}]
            #:automaton-simulation-de.rc{:queue []}]
           (-> {}
               (sut/queue-event 1 {:a :b})
               (unqueue-event-cacheproof 1))))
    (is (= [[#:automaton-simulation-de.rc{:seizing-event {:a :b}
                                          :consumed-quantity 17}]
            #:automaton-simulation-de.rc{:queue [#::sim-rc{:seizing-event {:a :c}
                                                           :consumed-quantity 19}
                                                 #:automaton-simulation-de.rc{:seizing-event {:d :b}
                                                                              :consumed-quantity
                                                                              11}]}]
           (-> {}
               (sut/queue-event 17 {:a :b})
               (sut/queue-event 19 {:a :c})
               (sut/queue-event 11 {:d :b})
               (unqueue-event-cacheproof 17)))))
  (testing "unqueue more than the remaining capacity"
    (is (= [[#:automaton-simulation-de.rc{:seizing-event {:a :b}
                                          :consumed-quantity 1}
             #:automaton-simulation-de.rc{:seizing-event {:d :b}
                                          :consumed-quantity 2}]
            #:automaton-simulation-de.rc{:queue []}]
           (-> {}
               (sut/queue-event 1 {:a :b})
               (sut/queue-event 2 {:d :b})
               (unqueue-event-cacheproof 5)))))
  (testing "unqueue less than the first capacity"
    (is (= [[]
            #:automaton-simulation-de.rc{:queue [#::sim-rc{:seizing-event {:a :b}
                                                           :consumed-quantity 10}
                                                 #:automaton-simulation-de.rc{:seizing-event {:d :b}
                                                                              :consumed-quantity
                                                                              20}]}]
           (-> {}
               (sut/queue-event 10 {:a :b})
               (sut/queue-event 20 {:d :b})
               (unqueue-event-cacheproof 5)))))
  (testing "unqueue exactly the expected capacity of two events"
    (is (= [[#:automaton-simulation-de.rc{:seizing-event {:a :b}
                                          :consumed-quantity 1}
             #:automaton-simulation-de.rc{:seizing-event {:d :b}
                                          :consumed-quantity 2}]
            #:automaton-simulation-de.rc{:queue []}]
           (-> {}
               (sut/queue-event 1 {:a :b})
               (sut/queue-event 2 {:d :b})
               (unqueue-event-cacheproof 3))))
    (is (= [[#:automaton-simulation-de.rc{:seizing-event {:a :b}
                                          :consumed-quantity 1}]
            #:automaton-simulation-de.rc{:queue [#::sim-rc{:seizing-event {:d :b}
                                                           :consumed-quantity 2}]}]
           (-> {}
               (sut/queue-event 1 {:a :b})
               (sut/queue-event 2 {:d :b})
               (unqueue-event-cacheproof 2))))
    (is (= [[#:automaton-simulation-de.rc{:seizing-event {:a :b}
                                          :consumed-quantity 1}
             #:automaton-simulation-de.rc{:seizing-event {:d :b}
                                          :consumed-quantity 2}]
            #:automaton-simulation-de.rc{:queue []}]
           (-> {}
               (sut/queue-event 1 {:a :b})
               (sut/queue-event 2 {:d :b})
               (unqueue-event-cacheproof 4))))
    (is (= [[#:automaton-simulation-de.rc{:seizing-event {:a :b}
                                          :consumed-quantity 1}
             #:automaton-simulation-de.rc{:seizing-event {:b :b}
                                          :consumed-quantity 2}]
            #:automaton-simulation-de.rc{:queue [#::sim-rc{:seizing-event {:c :b}
                                                           :consumed-quantity 4}
                                                 #:automaton-simulation-de.rc{:seizing-event {:d :b}
                                                                              :consumed-quantity
                                                                              2}]}]
           (-> {}
               (sut/queue-event 1 {:a :b})
               (sut/queue-event 2 {:b :b})
               (sut/queue-event 4 {:c :b})
               (sut/queue-event 2 {:d :b})
               (unqueue-event-cacheproof 4))))))
