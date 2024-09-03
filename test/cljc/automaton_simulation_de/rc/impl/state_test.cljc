(ns automaton-simulation-de.rc.impl.state-test
  (:require
   #?(:clj [clojure.test :refer [deftest is testing]]
      :cljs [cljs.test :refer [deftest is testing] :include-macros true])
   [automaton-core.utils.map                                  :as utils-map]
   [automaton-simulation-de.rc                                :as sim-rc]
   [automaton-simulation-de.rc.impl.preemption-policy.factory :as sim-rc-preemption-policy-factory]
   [automaton-simulation-de.rc.impl.resource-test             :as sim-rc-resource-test]
   [automaton-simulation-de.rc.impl.state                     :as sut]
   [automaton-simulation-de.rc.impl.unblocking-policy.factory :as sim-rc-unblocking-policy-factory]
   [automaton-simulation-de.simulation-engine                 :as-alias sim-engine]))

(defn uncache
  "For test purposes, removes the `cache` from the `resource` called `resource-name`."
  [state resource-name]
  (update-in state [::sim-rc/resource resource-name] sim-rc-resource-test/uncache))

(defn uncache-pair
  "For test purposes, in a pair, removes the `cache` from the `resource` called `resource-name`."
  [pair resource-name]
  (update pair 1 uncache resource-name))

(defn add-cache
  [state resource-name]
  (update-in state [::sim-rc/resource resource-name] sim-rc-resource-test/add-cache))

(defn update-resource-capacity-cacheproof
  [state resource-name new-capacity]
  (-> state
      (add-cache resource-name)
      (sut/update-resource-capacity resource-name new-capacity)
      (uncache-pair resource-name)))

(defn dispose-cacheproof
  "For test purposes, add default cache information to the resource `resource-name` and executes `dispose` on it."
  [state resource-name consumption-uuid]
  (-> state
      (add-cache resource-name)
      (sut/dispose resource-name consumption-uuid)
      (uncache-pair resource-name)))

(defn- remove-consumption-uuid
  "To remove randomness and ease testing"
  [state resource-name]
  (let [translation (-> state
                        (get-in [::sim-rc/resource resource-name ::sim-rc/currently-consuming])
                        utils-map/keys->sequence-number)]
    (-> state
        (update-in [::sim-rc/resource resource-name ::sim-rc/currently-consuming]
                   (fn [m] (utils-map/translate-keys m translation))))))

(deftest define-resources-test
  (testing "None resource is ok"
    (is (= #:automaton-simulation-de.rc{:resource {}}
           (sut/define-resources {} {} {} {})
           (sut/define-resources {} nil {} {}))))
  (testing "Existing resources are not modified"
    (is (= #:automaton-simulation-de.rc{:resource {::test #::sim-rc{:capacity 1}}}
           (sut/define-resources {::sim-rc/resource {::test #:automaton-simulation-de.rc{:capacity
                                                                                         1}}}
                                 {}
                                 {}
                                 {}))))
  (testing "A resource is updated"
    (is
     (=
      #:automaton-simulation-de.rc{:resource
                                   {::test
                                    #:automaton-simulation-de.rc{:capacity 1
                                                                 :name ::test
                                                                 :preemption-policy
                                                                 ::sim-rc/no-preemption
                                                                 :cache
                                                                 #:automaton-simulation-de.rc{:unblocking-policy-fn
                                                                                              sim-rc-unblocking-policy-factory/default-policy
                                                                                              :preemption-policy-fn
                                                                                              sim-rc-preemption-policy-factory/default-policy}
                                                                 :queue []
                                                                 :currently-consuming {}
                                                                 :unblocking-policy ::sim-rc/FIFO
                                                                 :renewable? true}}}
      (sut/define-resources {} {::test #:automaton-simulation-de.rc{:capacity 1}} {} {})))))

(deftest update-resource-capacity-test
  (testing "Empty resource is updated"
    (is (= [[]
            #:automaton-simulation-de.rc{:resource {::test #::sim-rc{:capacity 12
                                                                     :queue []}}}]
           (update-resource-capacity-cacheproof {} ::test 12))))
  (testing "Capacity is updated"
    (is (= [[]
            #:automaton-simulation-de.rc{:resource {::test #::sim-rc{:capacity 17
                                                                     :queue []}}}]
           (update-resource-capacity-cacheproof
            #:automaton-simulation-de.rc{:resource {::test #::sim-rc{:capacity 12}}}
            ::test
            17)))))

(deftest seize-test
  (testing "Nil event is skipped" (is (= [nil {:foo :bar}] (sut/seize {:foo :bar} ::test 1 nil))))
  (testing "Nil resource name is skipped"
    (is (= [nil {:foo :bar}]
           (sut/seize {:foo :bar}
                      nil
                      1
                      #:automaton-simulation-de.simulation-engine{:type ::a
                                                                  :date 4}))))
  (testing "Non existing event name is skipped"
    (is (= [nil {:foo :bar}]
           (sut/seize {:foo :bar}
                      ::not-existing-resource
                      1
                      #:automaton-simulation-de.simulation-engine{:type ::a
                                                                  :date 4}))))
  (testing "When capacity is big enough, it is pushed here"
    (is (= {::sim-rc/seizing-event #:automaton-simulation-de.simulation-engine{:type ::a
                                                                               :date 5}
            ::sim-rc/consumed-quantity 17}
           (let [[consumption-uuid resource]
                 (-> #:automaton-simulation-de.rc{:resource {::test #::sim-rc{:capacity 20}}}
                     (sut/seize ::test
                                17
                                #:automaton-simulation-de.simulation-engine{:type ::a
                                                                            :date 5}))]
             (get-in resource
                     [::sim-rc/resource ::test ::sim-rc/currently-consuming consumption-uuid])))))
  (testing
    "Moves the postponed event in the ::currently-seizing - in the case another event is already there"
    (is
     (=
      #:automaton-simulation-de.rc{:resource
                                   {::test
                                    #:automaton-simulation-de.rc{:currently-consuming
                                                                 {1
                                                                  #:automaton-simulation-de.rc{:seizing-event
                                                                                               #:automaton-simulation-de.simulation-engine{:type
                                                                                                                                           ::a
                                                                                                                                           :date
                                                                                                                                           5}
                                                                                               :consumed-quantity
                                                                                               9}
                                                                  2
                                                                  #:automaton-simulation-de.rc{:seizing-event
                                                                                               #:automaton-simulation-de.simulation-engine{:type
                                                                                                                                           ::b
                                                                                                                                           :date
                                                                                                                                           7}
                                                                                               :consumed-quantity
                                                                                               17}}
                                                                 :capacity 30}}}
      (-> #:automaton-simulation-de.rc{:resource {::test #::sim-rc{:capacity 30}}}
          (sut/seize ::test
                     9
                     #:automaton-simulation-de.simulation-engine{:type ::a
                                                                 :date 5})
          second
          (sut/seize ::test
                     17
                     #:automaton-simulation-de.simulation-engine{:type ::b
                                                                 :date 7})
          second
          (remove-consumption-uuid ::test))))))

(deftest dispose-capacity-test
  (testing "Disposing unknown resource is working"
    (is
     (= [[]
         {::sim-rc/resource {::a #:automaton-simulation-de.rc{:currently-consuming {}
                                                              :queue []}}}]
        (dispose-cacheproof {}
                            ::a
                            #:automaton-simulation-de.simulation-engine{:type :a
                                                                        :date 1})
        (dispose-cacheproof {::sim-rc/resource {::a {::sim-rc/currently-consuming {}}}} ::a 666))))
  (testing "Disposing a currently consuming event is removed"
    (is (= [[]
            {::sim-rc/resource {::test #:automaton-simulation-de.rc{:currently-consuming {}
                                                                    :queue []
                                                                    :capacity 30}}}]
           (-> #:automaton-simulation-de.rc{:resource {::test #::sim-rc{:capacity 30}}}
               (sut/seize ::test
                          12
                          #:automaton-simulation-de.simulation-engine{:type :a
                                                                      :date 1})
               second
               (remove-consumption-uuid ::test)
               (dispose-cacheproof ::test 1)))))
  (testing "A non existing resource consumption uuid doesn't change currently seizing list"
    (is
     (=
      [[]
       #:automaton-simulation-de.rc{:resource
                                    {::test
                                     #:automaton-simulation-de.rc{:currently-consuming
                                                                  {#uuid
                                                                    "33497220-f844-11ee-9fa1-17acea14e9df"
                                                                   {:a :b}}}}}]
      (->
        #:automaton-simulation-de.rc{:resource
                                     {::test
                                      #:automaton-simulation-de.rc{:currently-consuming
                                                                   {#uuid
                                                                     "33497220-f844-11ee-9fa1-17acea14e9df"
                                                                    {:a :b}}}}}
        (dispose-cacheproof ::test #uuid "33497220-f844-11ee-9fa1-17acea14e9de")))))
  (testing "Disposing known resource is removed from list - case when others are still in the list"
    (is
     (=
      [[#:automaton-simulation-de.rc{:seizing-event {:c :b}
                                     :consumed-quantity 4}]
       #:automaton-simulation-de.rc{:resource
                                    {::test
                                     #:automaton-simulation-de.rc{:currently-consuming
                                                                  {2
                                                                   #:automaton-simulation-de.rc{:seizing-event
                                                                                                {:b
                                                                                                 :b}
                                                                                                :consumed-quantity
                                                                                                9}}
                                                                  :queue
                                                                  [#:automaton-simulation-de.rc{:seizing-event
                                                                                                {:d
                                                                                                 :b}
                                                                                                :consumed-quantity
                                                                                                5}]
                                                                  :capacity 17}}}]
      (-> #:automaton-simulation-de.rc{:resource {::test #::sim-rc{:capacity 17}}}
          (sut/seize ::test 7 {:a :b})
          second
          (sut/seize ::test 9 {:b :b})
          second
          (sut/seize ::test 4 {:c :b})
          second
          (sut/seize ::test 5 {:d :b})
          second
          (remove-consumption-uuid ::test)
          (dispose-cacheproof ::test 1))))))
