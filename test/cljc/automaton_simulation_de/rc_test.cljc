(ns automaton-simulation-de.rc-test
  (:require
   #?(:clj [clojure.test :refer [deftest is testing]]
      :cljs [cljs.test :refer [deftest is testing] :include-macros true])
   [automaton-core.utils.map                       :as utils-map]
   [automaton-simulation-de.impl.model             :as sim-de-model]
   [automaton-simulation-de.rc                     :as sut]
   [automaton-simulation-de.rc.impl.resource       :as sim-de-rc-resource]
   [automaton-simulation-de.rc.impl.state-test     :as sim-de-rc-state-test]
   [automaton-simulation-de.scheduler.event        :as sim-de-event]
   [automaton-simulation-de.scheduler.event-return :as sim-de-event-return]
   [automaton-simulation-de.scheduler.snapshot     :as sim-de-snapshot]))

(defn- default-resource-event-return
  ([resource-name] (default-resource-event-return resource-name nil))
  ([resource-name capacity]
   (let [resource (cond-> (sim-de-rc-resource/defaulting-values nil {} {})
                    (not (nil? capacity)) (assoc ::sut/capacity capacity))]
     (sim-de-event-return/build {::sut/resource {resource-name resource}} []))))

(defn uncache
  "For test purposes, remove cache values."
  [event-return resource-name]
  (update event-return
          ::sim-de-event-return/state
          sim-de-rc-state-test/uncache
          resource-name))

(defn add-cache
  "For test purposes, adds default cache values."
  [event-return resource-name]
  (update event-return
          ::sim-de-event-return/state
          sim-de-rc-state-test/add-cache
          resource-name))

(defn- resource-update-cacheproof
  [event-return resource-name new-capacity]
  (-> event-return
      (add-cache resource-name)
      (sut/resource-update resource-name new-capacity)
      (uncache resource-name)))

(defn- dispose-cacheproof
  [event-return resource-name current-event]
  (-> event-return
      (add-cache resource-name)
      (sut/dispose resource-name current-event)
      (uncache resource-name)))

(defn- remove-consumption-uuid
  [event-return resource-name]
  (let [translation (-> event-return
                        (get-in [::sim-de-event-return/state
                                 ::sut/resource
                                 resource-name
                                 ::sut/currently-consuming])
                        utils-map/keys->sequence-number)]
    (-> event-return
        (update-in [::sim-de-event-return/state
                    ::sut/resource
                    resource-name
                    ::sut/currently-consuming]
                   (fn [m] (utils-map/translate-keys m translation)))
        (update ::sim-de-event-return/future-events
                (fn [future-events]
                  (mapv (fn [future-event]
                          (update-in future-event
                                     [::sut/resource resource-name]
                                     (fn [x] (get translation x x))))
                        future-events))))))

(deftest seizing-resource-test
  (testing "Non existing resource"
    (testing "Seizing a non existing resource doesn't change the event return"
      (is (= {:a :b} (sut/seize {:a :b} ::test 1 1 {})))))
  (testing "Unaivalable resource"
    (testing
      "Seizing an unavailable resource blocks the postponed-event, so it is added in the resource queue"
      (is (= 1
             (-> (default-resource-event-return ::test)
                 (sut/seize ::test 2 1 (sim-de-event/make-event :a 1))
                 ::sim-de-event-return/state
                 ::sut/resource
                 ::test
                 ::sut/queue
                 count)))
      (is (= 2
             (-> (default-resource-event-return ::test)
                 (sut/seize ::test 2 1 (sim-de-event/make-event :a 2))
                 (sut/seize ::test 2 1 (sim-de-event/make-event :b 3))
                 ::sim-de-event-return/state
                 ::sut/resource
                 ::test
                 ::sut/queue
                 count))))
    (testing "Seizing an unavailable resource don't create new future-events"
      (is (= 2
             (-> (default-resource-event-return ::test 0)
                 (sim-de-event-return/add-events
                  (sim-de-event/make-events :a 1 :b 2))
                 (sut/seize ::test 1 1 {:a :b})
                 ::sim-de-event-return/future-events
                 count)))))
  (testing "Available resource"
    (testing
      "Seizing an available resource launch the event execution, by adding the postponed-event in future-events (and not removing existing ones)"
      (is (= 4
             (-> (default-resource-event-return ::test 20)
                 (sim-de-event-return/add-events
                  (sim-de-event/make-events :a 5 :b 2 :c 4))
                 (sut/seize ::test 1 1 {:a :b})
                 ::sim-de-event-return/future-events
                 count))))
    (testing
      "When seizing an available resource, the future-events is containing the consumption-uuid"
      (is (-> (default-resource-event-return ::test 10)
              (sut/seize ::test 1 1 (sim-de-event/make-event :a 1))
              ::sim-de-event-return/future-events
              first
              ::sut/resource
              ::test
              uuid?))))
  (testing
    "Seizing a non available event doesn't generates it, so it creates no new future-event"
    (is (= 3
           (-> (sut/seize (-> (default-resource-event-return ::test 0)
                              (sim-de-event-return/add-events
                               (sim-de-event/make-events :a 5 :c 2 :a 4)))
                          ::test 1
                          1 (sim-de-event/make-event :a 1))
               ::sim-de-event-return/future-events
               count)))))

(deftest resource-dispose-test
  (testing "Disposing a non existing resource is noop"
    (is (= #::sim-de-event-return{:state nil}
           (sut/dispose {} ::a (sim-de-event/make-event :a 1)))))
  (testing "Disposing an existing resource, currently consuming is removing it"
    (is
     (= #::sim-de-event-return{:state #::sut{:resource
                                             {::test #::sut{:currently-consuming
                                                            {}
                                                            :queue []}}}
                               :future-events []}
        (-> #::sim-de-event-return{:state
                                   #::sut{:resource
                                          {::test
                                           #::sut{:currently-consuming
                                                  {1
                                                   #::sut{:seizing-event {:a :b}
                                                          :consumed-quantity 1}}
                                                  :queue []}}}
                                   :future-events []}
            (dispose-cacheproof ::test {::sut/resource {::test 1}})))))
  (testing "Disposing a resource with blocked events release them"
    (is
     (=
      #::sim-de-event-return{:state
                             #::sut{:resource
                                    {::test
                                     #::sut{:currently-consuming
                                            {1 #::sut{:seizing-event
                                                      #::sim-de-event{:type :a
                                                                      :date 2}
                                                      :consumed-quantity 2}}
                                            :capacity 3
                                            :preemption-policy
                                            ::sut/no-preemption
                                            :renewable? true
                                            :unblocking-policy ::sut/FIFO
                                            :queue
                                            [#::sut{:seizing-event
                                                    {::sim-de-event/type :b
                                                     ::sim-de-event/date 2}
                                                    :consumed-quantity 3}]}}}
                             :future-events [{::sim-de-event/type :a
                                              ::sim-de-event/date 2
                                              ::sut/resource {::test 1}}]}
      (let [event-return
            (-> (default-resource-event-return ::test 3)
                (sut/seize ::test 2 2 (sim-de-event/make-event :a 2))
                (sut/seize ::test 3 2 (sim-de-event/make-event :b 2)))
            a-currently-consuming-event (-> event-return
                                            ::sim-de-event-return/future-events
                                            first)]
        (-> event-return
            (remove-consumption-uuid ::test)
            (dispose-cacheproof ::test a-currently-consuming-event)))))))

(deftest resource-update-test
  (testing "Non existing resource is created"
    (is
     (=
      #::sim-de-event-return{:state #::sut{:resource {::test #::sut{:capacity 7
                                                                    :queue []}}}
                             :future-events []}
      (resource-update-cacheproof (sim-de-event-return/build {} []) ::test 7))))
  (testing "Existing resource is updated"
    (is (= #::sim-de-event-return{:state #::sut{:resource {::test
                                                           #::sut{:capacity 7
                                                                  :queue []}}}
                                  :future-events []}
           (-> #::sim-de-event-return{:state #::sut{:resource
                                                    {::test #::sut{:capacity 5
                                                                   :queue []}}}
                                      :future-events []}
               (resource-update-cacheproof ::test 7))))))

(defn- wo-initial-snapshot
  [model]
  (dissoc model ::sim-de-model/initial-snapshot))

(defn- resources-kw
  [model]
  (-> model
      (get-in
       [::sim-de-model/initial-snapshot ::sim-de-snapshot/state ::sut/resource])
      keys
      set))

(deftest wrap-model-test
  (testing "If no resource is defined, doesn't change the model"
    (is (nil? (sut/wrap-model nil nil nil nil)))
    (is (nil? (sut/wrap-model nil {} nil nil))))
  (testing "Resources are added"
    (is (= [{:a :b} #{:ra :rb}]
           ((juxt wo-initial-snapshot resources-kw)
            (-> {:a :b}
                (sut/wrap-model {:rc {:ra nil
                                      :rb {}}}
                                nil
                                nil))))))
  (testing "Existing data are not overidden"
    (is (= [{:a :b} #{:ra :rb :rc :rd}]
           ((juxt wo-initial-snapshot resources-kw)
            (-> {:a :b
                 ::sim-de-model/initial-snapshot {::sim-de-snapshot/state
                                                  {::sut/resource {:ra :ra
                                                                   :rb :rb}}}}
                (sut/wrap-model {:rc {:rc nil
                                      :rd {}}}
                                nil
                                nil)))))))
