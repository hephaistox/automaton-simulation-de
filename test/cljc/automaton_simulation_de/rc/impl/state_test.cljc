(ns automaton-simulation-de.rc.impl.state-test
  (:require
   #?(:clj [clojure.test :refer [deftest is testing]]
      :cljs [cljs.test :refer [deftest is testing] :include-macros true])
   [automaton-core.utils.map                                  :as utils-map]
   [automaton-simulation-de.rc                                :as sim-de-rc]
   [automaton-simulation-de.rc.impl.preemption-policy.factory
    :as sim-de-rc-preemption-policy-factory]
   [automaton-simulation-de.rc.impl.resource-test
    :as sim-de-rc-resource-test]
   [automaton-simulation-de.rc.impl.state                     :as sut]
   [automaton-simulation-de.rc.impl.unblocking-policy.factory
    :as sim-de-rc-unblocking-policy-factory]
   [automaton-simulation-de.scheduler.event                   :as
                                                              sim-de-event]))

(defn uncache
  "For test purposes, removes the `cache` from the `resource` called `resource-name`."
  [state resource-name]
  (update-in state
             [:automaton-simulation-de.rc/resource resource-name]
             sim-de-rc-resource-test/uncache))

(defn uncache-pair
  "For test purposes, in a pair, removes the `cache` from the `resource` called `resource-name`."
  [pair resource-name]
  (update pair 1 uncache resource-name))

(defn add-cache
  [state resource-name]
  (update-in state
             [:automaton-simulation-de.rc/resource resource-name]
             sim-de-rc-resource-test/add-cache))

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
                        (get-in [::sim-de-rc/resource
                                 resource-name
                                 ::sim-de-rc/currently-consuming])
                        utils-map/keys->sequence-number)]
    (-> state
        (update-in
         [::sim-de-rc/resource resource-name ::sim-de-rc/currently-consuming]
         (fn [m] (utils-map/translate-keys m translation))))))

(deftest define-resources-test
  (testing "None resource is ok"
    (is (= #::sim-de-rc{:resource {}}
           (sut/define-resources {} {} {} {})
           (sut/define-resources {} nil {} {}))))
  (testing "Existing resources are not modified"
    (is (= #::sim-de-rc{:resource {::test #::sim-de-rc{:capacity 1}}}
           (sut/define-resources {::sim-de-rc/resource
                                  {::test #::sim-de-rc{:capacity 1}}}
                                 {}
                                 {}
                                 {}))))
  (testing "A resource is updated"
    (is
     (=
      #::sim-de-rc{:resource
                   {::test
                    #::sim-de-rc{:capacity 1
                                 :name ::test
                                 :preemption-policy ::sim-de-rc/no-preemption
                                 :cache
                                 #::sim-de-rc{:unblocking-policy-fn
                                              sim-de-rc-unblocking-policy-factory/default-policy
                                              :preemption-policy-fn
                                              sim-de-rc-preemption-policy-factory/default-policy}
                                 :queue []
                                 :currently-consuming {}
                                 :unblocking-policy ::sim-de-rc/FIFO
                                 :renewable? true}}}
      (sut/define-resources {} {::test #::sim-de-rc{:capacity 1}} {} {})))))

(deftest update-resource-capacity-test
  (testing "Empty resource is updated"
    (is (= [[]
            #::sim-de-rc{:resource {::test #::sim-de-rc{:capacity 12
                                                        :queue []}}}]
           (update-resource-capacity-cacheproof {} ::test 12))))
  (testing "Capacity is updated"
    (is (= [[]
            #::sim-de-rc{:resource {::test #::sim-de-rc{:capacity 17
                                                        :queue []}}}]
           (update-resource-capacity-cacheproof
            #::sim-de-rc{:resource {::test #::sim-de-rc{:capacity 12}}}
            ::test
            17)))))

(deftest seize-test
  (testing "Nil event is skipped"
    (is (= [nil {:foo :bar}] (sut/seize {:foo :bar} ::test 1 nil))))
  (testing "Nil resource name is skipped"
    (is (= [nil {:foo :bar}]
           (sut/seize {:foo :bar} nil 1 (sim-de-event/make-event ::a 4)))))
  (testing "Non existing event name is skipped"
    (is (= [nil {:foo :bar}]
           (sut/seize {:foo :bar}
                      ::not-existing-resource
                      1
                      (sim-de-event/make-event ::a 4)))))
  (testing "When capacity is big enough, it is pushed here"
    (is (= {::sim-de-rc/seizing-event #::sim-de-event{:type ::a
                                                      :date 5}
            ::sim-de-rc/consumed-quantity 17}
           (let [[consumption-uuid resource]
                 (-> #::sim-de-rc{:resource {::test #::sim-de-rc{:capacity 20}}}
                     (sut/seize ::test 17 (sim-de-event/make-event ::a 5)))]
             (get-in resource
                     [::sim-de-rc/resource
                      ::test
                      ::sim-de-rc/currently-consuming
                      consumption-uuid])))))
  (testing
    "Moves the postponed event in the ::currently-seizing - in the case another event is already there"
    (is
     (= #::sim-de-rc{:resource
                     {::test
                      #::sim-de-rc{:currently-consuming
                                   {1 #::sim-de-rc{:seizing-event
                                                   #::sim-de-event{:type ::a
                                                                   :date 5}
                                                   :consumed-quantity 9}
                                    2 #::sim-de-rc{:seizing-event
                                                   #::sim-de-event{:type ::b
                                                                   :date 7}
                                                   :consumed-quantity 17}}
                                   :capacity 30}}}
        (-> #::sim-de-rc{:resource {::test #::sim-de-rc{:capacity 30}}}
            (sut/seize ::test 9 (sim-de-event/make-event ::a 5))
            second
            (sut/seize ::test 17 (sim-de-event/make-event ::b 7))
            second
            (remove-consumption-uuid ::test))))))

(deftest dispose-capacity-test
  (testing "Disposing unknown resource is working"
    (is (= [[]
            {::sim-de-rc/resource {::a #::sim-de-rc{:currently-consuming {}
                                                    :queue []}}}]
           (dispose-cacheproof {} ::a (sim-de-event/make-event :a 1))
           (dispose-cacheproof {::sim-de-rc/resource
                                {::a {::sim-de-rc/currently-consuming {}}}}
                               ::a
                               666))))
  (testing "Disposing a currently consuming event is removed"
    (is (= [[]
            {::sim-de-rc/resource {::test #::sim-de-rc{:currently-consuming {}
                                                       :queue []
                                                       :capacity 30}}}]
           (-> #::sim-de-rc{:resource {::test #::sim-de-rc{:capacity 30}}}
               (sut/seize ::test 12 (sim-de-event/make-event :a 1))
               second
               (remove-consumption-uuid ::test)
               (dispose-cacheproof ::test 1)))))
  (testing
    "A non existing resource consumption uuid doesn't change currently seizing list"
    (is
     (=
      [[]
       #::sim-de-rc{:resource
                    {::test
                     #::sim-de-rc{:currently-consuming
                                  {#uuid "33497220-f844-11ee-9fa1-17acea14e9df"
                                   {:a :b}}}}}]
      (-> #::sim-de-rc{:resource
                       {::test
                        #::sim-de-rc{:currently-consuming
                                     {#uuid
                                       "33497220-f844-11ee-9fa1-17acea14e9df"
                                      {:a :b}}}}}
          (dispose-cacheproof ::test
                              #uuid "33497220-f844-11ee-9fa1-17acea14e9de")))))
  (testing
    "Disposing known resource is removed from list - case when others are still in the list"
    (is
     (= [[#::sim-de-rc{:seizing-event {:c :b}
                       :consumed-quantity 4}]
         #::sim-de-rc{:resource
                      {::test
                       #::sim-de-rc{:currently-consuming
                                    {2 #::sim-de-rc{:seizing-event {:b :b}
                                                    :consumed-quantity 9}}
                                    :queue [#::sim-de-rc{:seizing-event {:d :b}
                                                         :consumed-quantity 5}]
                                    :capacity 17}}}]
        (-> #::sim-de-rc{:resource {::test #::sim-de-rc{:capacity 17}}}
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
