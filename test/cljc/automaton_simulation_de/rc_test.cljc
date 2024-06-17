(ns automaton-simulation-de.rc-test
  (:require
   #?(:clj [clojure.test :refer [deftest is testing]]
      :cljs [cljs.test :refer [deftest is testing] :include-macros true])
   [automaton-core.utils.map                               :as utils-map]
   [automaton-simulation-de.rc                             :as sut]
   [automaton-simulation-de.rc.impl.resource               :as
                                                           sim-de-rc-resource]
   [automaton-simulation-de.rc.impl.state-test             :as
                                                           sim-de-rc-state-test]
   [automaton-simulation-de.simulation-engine              :as-alias sim-engine]
   [automaton-simulation-de.simulation-engine.event-return
    :as sim-de-event-return]))

(defn- default-resource-event-return
  ([resource-name] (default-resource-event-return resource-name nil))
  ([resource-name capacity]
   (let [resource (cond-> (sim-de-rc-resource/defaulting-values nil {} {})
                    (not (nil? capacity)) (assoc ::sut/capacity capacity))]
     #:automaton-simulation-de.simulation-engine{:state {::sut/resource
                                                         {resource-name
                                                          resource}}
                                                 :future-events []})))

(defn uncache
  "For test purposes, remove cache values."
  [event-return resource-name]
  (update event-return
          ::sim-engine/state
          sim-de-rc-state-test/uncache
          resource-name))

(defn add-cache
  "For test purposes, adds default cache values."
  [event-return resource-name]
  (update event-return
          ::sim-engine/state
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
                        (get-in [::sim-engine/state
                                 ::sut/resource
                                 resource-name
                                 ::sut/currently-consuming])
                        utils-map/keys->sequence-number)]
    (-> event-return
        (update-in [::sim-engine/state
                    ::sut/resource
                    resource-name
                    ::sut/currently-consuming]
                   (fn [m] (utils-map/translate-keys m translation)))
        (update ::sim-engine/future-events
                (fn [future-events]
                  (mapv (fn [future-event]
                          (update-in future-event
                                     [::sut/resource resource-name]
                                     (fn [x] (get translation x x))))
                        future-events))))))

(deftest seizing-resource-test
  (testing "Non existing resource"
    (is (= {:a :b} (sut/seize {:a :b} ::test 1 1 {}))
        "Seizing a non existing resource doesn't change the event return"))
  (testing "Unaivalable resource"
    (is
     (= 1
        (-> (-> (default-resource-event-return ::test)
                (sut/seize
                 ::test 2
                 1 #:automaton-simulation-de.simulation-engine{:type :a
                                                               :date 1}))
            ::sim-engine/state
            ::sut/resource
            ::test
            ::sut/queue
            count))
     "Seizing an unavailable resource blocks the postponed-event, so it is added in the resource queue")
    (is (= 2
           (-> (default-resource-event-return ::test 0)
               (sim-de-event-return/add-events
                [#:automaton-simulation-de.simulation-engine{:type :a
                                                             :date 1}
                 #:automaton-simulation-de.simulation-engine{:type :b
                                                             :date 2}])
               (sut/seize ::test 1 1 {:a :b})
               ::sim-engine/future-events
               count))
        "Seizing an unavailable resource don't create new future-events"))
  (testing "Available resource"
    (is
     (= 4
        (-> (default-resource-event-return ::test 20)
            (sim-de-event-return/add-events
             [#:automaton-simulation-de.simulation-engine{:type :a
                                                          :date 5}
              #:automaton-simulation-de.simulation-engine{:type :b
                                                          :date 2}
              #:automaton-simulation-de.simulation-engine{:type :c
                                                          :date 4}])
            (sut/seize ::test 1 1 {:a :b})
            ::sim-engine/future-events
            count))
     "Seizing an available resource launch the event execution, by adding the postponed-event in future-events (and not removing existing ones)")
    (is
     (-> (default-resource-event-return ::test 10)
         (sut/seize ::test 1
                    1 #:automaton-simulation-de.simulation-engine{:type :a
                                                                  :date 1})
         ::sim-engine/future-events
         first
         ::sut/resource
         ::test
         uuid?)
     "When seizing an available resource, the future-events is containing the consumption-uuid"))
  (is
   (= 3
      (-> (sut/seize (-> (default-resource-event-return ::test 0)
                         (sim-de-event-return/add-events
                          [#:automaton-simulation-de.simulation-engine{:type :a
                                                                       :date 5}
                           #:automaton-simulation-de.simulation-engine{:type :c
                                                                       :date 2}
                           #:automaton-simulation-de.simulation-engine{:type :a
                                                                       :date
                                                                       4}]))
                     ::test 1
                     1 #:automaton-simulation-de.simulation-engine{:type :a
                                                                   :date 1})
          ::sim-engine/future-events
          count))
   "Seizing a non available event doesn't generates it, so it creates no new future-event"))

(deftest resource-dispose-test
  (is (= #:automaton-simulation-de.simulation-engine{:state nil}
         (sut/dispose {}
                      ::a
                      #:automaton-simulation-de.simulation-engine{:type :a
                                                                  :date 1}))
      "Disposing a non existing resource is noop")
  (is
   (=
    #:automaton-simulation-de.simulation-engine{:state
                                                #::sut{:resource
                                                       {::test
                                                        #::sut{:currently-consuming
                                                               {}
                                                               :queue []}}}
                                                :future-events []}
    (->
      #:automaton-simulation-de.simulation-engine{:state
                                                  #::sut{:resource
                                                         {::test
                                                          #::sut{:currently-consuming
                                                                 {1
                                                                  #::sut{:seizing-event
                                                                         {:a :b}
                                                                         :consumed-quantity
                                                                         1}}
                                                                 :queue []}}}
                                                  :future-events []}
      (dispose-cacheproof ::test {::sut/resource {::test 1}})))
   "Disposing an existing resource, currently consuming is removing it")
  (is
   (=
    #:automaton-simulation-de.simulation-engine{:state
                                                #::sut{:resource
                                                       {::test
                                                        #::sut{:currently-consuming
                                                               {1
                                                                #::sut{:seizing-event
                                                                       #:automaton-simulation-de.simulation-engine{:type
                                                                                                                   :a
                                                                                                                   :date
                                                                                                                   2}
                                                                       :consumed-quantity
                                                                       2}}
                                                               :capacity 3
                                                               :preemption-policy
                                                               ::sut/no-preemption
                                                               :renewable? true
                                                               :unblocking-policy
                                                               ::sut/FIFO
                                                               :queue
                                                               [#::sut{:seizing-event
                                                                       #:automaton-simulation-de.simulation-engine{:type
                                                                                                                   :b
                                                                                                                   :date
                                                                                                                   2}
                                                                       :consumed-quantity
                                                                       3}]}}}
                                                :future-events
                                                [#:automaton-simulation-de.simulation-engine{:type
                                                                                             :a
                                                                                             :date
                                                                                             2
                                                                                             ::sut/resource
                                                                                             {::test
                                                                                              1}}]}
    (let [event-return
          (-> (default-resource-event-return ::test 3)
              (sut/seize ::test 2
                         2 #:automaton-simulation-de.simulation-engine{:type :a
                                                                       :date 2})
              (sut/seize ::test 3
                         2 #:automaton-simulation-de.simulation-engine{:type :b
                                                                       :date
                                                                       2}))
          a-currently-consuming-event (-> event-return
                                          ::sim-engine/future-events
                                          first)]
      (-> event-return
          (remove-consumption-uuid ::test)
          (dispose-cacheproof ::test a-currently-consuming-event))))
   "Disposing a resource with blocked events release them"))

(deftest resource-update-test
  (is (= #:automaton-simulation-de.simulation-engine{:state
                                                     #::sut{:resource
                                                            {::test
                                                             #::sut{:capacity 7
                                                                    :queue []}}}
                                                     :future-events []}
         (resource-update-cacheproof
          #:automaton-simulation-de.simulation-engine{:state {}
                                                      :future-events []}
          ::test
          7))
      "Non existing resource is created")
  (is (=
       #:automaton-simulation-de.simulation-engine{:state
                                                   #::sut{:resource
                                                          {::test
                                                           #::sut{:capacity 7
                                                                  :queue []}}}
                                                   :future-events []}
       (->
         #:automaton-simulation-de.simulation-engine{:state
                                                     #::sut{:resource
                                                            {::test
                                                             #::sut{:capacity 5
                                                                    :queue []}}}
                                                     :future-events []}
         (resource-update-cacheproof ::test 7)))
      "Existing resource is updated"))

(defn- wo-initial-snapshot [model] (dissoc model ::sim-engine/initial-snapshot))

(defn- resources-kw
  [model]
  (-> model
      (get-in [::sim-engine/initial-snapshot ::sim-engine/state ::sut/resource])
      keys
      set))

(deftest wrap-model-test
  (testing "If no resource is defined, doesn't change the model"
    (is (nil? (sut/wrap-model nil nil nil nil)))
    (is (nil? (sut/wrap-model nil {} nil nil))))
  (is (= [{:a :b} #{:ra :rb}]
         ((juxt wo-initial-snapshot resources-kw)
          (-> {:a :b}
              (sut/wrap-model {:rc {:ra nil
                                    :rb {}}}
                              nil
                              nil))))
      "Resources are added")
  (is (= [{:a :b} #{:ra :rb :rc :rd}]
         ((juxt wo-initial-snapshot resources-kw)
          (-> {:a :b
               ::sim-engine/initial-snapshot {::sim-engine/state {::sut/resource
                                                                  {:ra :ra
                                                                   :rb :rb}}}}
              (sut/wrap-model {:rc {:rc nil
                                    :rd {}}}
                              nil
                              nil))))
      "Existing data are not overidden"))
