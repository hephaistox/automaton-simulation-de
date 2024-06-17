(ns automaton-simulation-de.rc.impl.resource-test
  (:require
   [automaton-simulation-de.rc                          :as sim-rc]
   #?(:clj [clojure.test :refer [deftest is testing]]
      :cljs [cljs.test :refer [deftest is testing] :include-macros true])
   [automaton-simulation-de.rc.impl.resource            :as sut]
   [automaton-simulation-de.rc.impl.resource.queue-test
    :as sim-de-resource-queue-test]
   [automaton-simulation-de.simulation-engine           :as-alias sim-engine]))

(defn uncache
  "For test purposes, remove the `cache` from the `resource`."
  [resource]
  (sim-de-resource-queue-test/uncache resource))

(defn add-cache
  "For test purposes, add a `cache` and its default values to the `resource`."
  [resource]
  (sim-de-resource-queue-test/add-cache resource))

(defn- dispose-cacheproof
  "Checking cache content is not the concern of this namespace, so it is removed.
  Adds the default fn in the cache that are needed to executed."
  [resource consumption-uuid]
  (-> resource
      add-cache
      (sut/dispose consumption-uuid)
      (update 1 uncache)))

(defn- update-capacity-cacheproof
  "Adds the default fn in the cache that are needed to executed."
  [resource new-capacity]
  (-> resource
      add-cache
      (sut/update-capacity new-capacity)
      (update 1 uncache)))

(deftest defaulting-values-test
  (testing "A purely defaulted value resource"
    (is (= #:automaton-simulation-de.rc{:capacity 1
                                        :currently-consuming {}
                                        :preemption-policy
                                        ::sim-rc/no-preemption
                                        :queue []
                                        :renewable? true
                                        :unblocking-policy ::sim-rc/FIFO}
           (uncache (sut/defaulting-values nil {} {})))))
  (testing "Other data are kept"
    (is (= #:automaton-simulation-de.rc{:capacity 1
                                        :currently-consuming {}
                                        :preemption-policy
                                        ::sim-rc/no-preemption
                                        :queue []
                                        :renewable? true
                                        :unblocking-policy ::sim-rc/FIFO
                                        :other-keys :are-allowed}
           (uncache (sut/defaulting-values
                     #:automaton-simulation-de.rc{:other-keys :are-allowed}
                     {}
                     {}))))))

(deftest nb-consumed-capacity-test
  (testing "With no currently seized, all the capacity is available"
    (is (zero? (sut/nb-consumed-resources nil)))
    (is (zero? (sut/nb-consumed-resources #:automaton-simulation-de.rc{:queue
                                                                       []}))))
  (testing "Defaulted consumed-quantity to 1"
    (is (= 1
           (sut/nb-consumed-resources
            #:automaton-simulation-de.rc{:currently-consuming {:a {:a :b}}}))))
  (testing "Currently seized are summed up"
    (is (=
         8
         (sut/nb-consumed-resources
          #:automaton-simulation-de.rc{:currently-consuming
                                       {#uuid
                                         "33497220-f844-11ee-9fa1-17acea14e9df"
                                        {::sim-rc/consumed-quantity 3}
                                        #uuid
                                         "33497220-f844-11ee-9fa1-17acea14e9de"
                                        {::sim-rc/consumed-quantity 5}}})))))

(deftest nb-available-resources-test
  (testing "A non existing resource capacity is defaulted to 1"
    (is (= (sut/nb-available-resources nil) 1)))
  (testing "With no currently seized, all the capacity is available"
    (is (= 7
           (sut/nb-available-resources #:automaton-simulation-de.rc{:capacity
                                                                    7}))))
  (testing
    "Currently seized events are deduced from capacity, their seized quantity is taken into account"
    (is
     (=
      9
      (sut/nb-available-resources
       #:automaton-simulation-de.rc{:capacity 17
                                    :currently-consuming
                                    {#uuid
                                      "33497220-f844-11ee-9fa1-17acea14e9df"
                                     {::sim-rc/event
                                      #:automaton-simulation-de.simulation-engine{:type
                                                                                  ::c
                                                                                  :date
                                                                                  11}
                                      ::sim-rc/consumed-quantity 3}
                                     #uuid
                                      "33497220-f844-11ee-9fa1-17acea14e9de"
                                     {::sim-rc/event
                                      #:automaton-simulation-de.simulation-engine{:type
                                                                                  ::d
                                                                                  :date
                                                                                  19}
                                      ::sim-rc/consumed-quantity 5}}}))))
  (testing "If all resources are used, zero are available"
    (is
     (zero?
      (sut/nb-available-resources
       #:automaton-simulation-de.rc{:capacity 8
                                    :currently-consuming
                                    {#uuid
                                      "33497220-f844-11ee-9fa1-17acea14e9df"
                                     {::sim-rc/event
                                      #:automaton-simulation-de.simulation-engine{:type
                                                                                  ::c
                                                                                  :date
                                                                                  11}
                                      ::sim-rc/consumed-quantity 3}
                                     #uuid
                                      "33497220-f844-11ee-9fa1-17acea14e9de"
                                     {::sim-rc/event
                                      #:automaton-simulation-de.simulation-engine{:type
                                                                                  ::d
                                                                                  :date
                                                                                  19}
                                      ::sim-rc/consumed-quantity 5}}})))
    (is
     (zero?
      (sut/nb-available-resources
       #:automaton-simulation-de.rc{:capacity 7
                                    :currently-consuming
                                    {#uuid
                                      "33497220-f844-11ee-9fa1-17acea14e9df"
                                     {::sim-rc/event
                                      #:automaton-simulation-de.simulation-engine{:type
                                                                                  ::c
                                                                                  :date
                                                                                  11}
                                      ::sim-rc/consumed-quantity 3}
                                     #uuid
                                      "33497220-f844-11ee-9fa1-17acea14e9de"
                                     {::sim-rc/event
                                      #:automaton-simulation-de.simulation-engine{:type
                                                                                  ::d
                                                                                  :date
                                                                                  19}
                                      ::sim-rc/consumed-quantity 5}}})))))

(deftest seize-test
  (testing "Seizing one resource with that capacity already available"
    (is
     (= #:automaton-simulation-de.rc{:seizing-event {:a :b}
                                     :consumed-quantity 9}
        (let [[consumption-uuid resource]
              (sut/seize #:automaton-simulation-de.rc{:capacity 13} 9 {:a :b})]
          (get-in resource [::sim-rc/currently-consuming consumption-uuid]))))
    (is
     (= #:automaton-simulation-de.rc{:seizing-event {:a :b}
                                     :consumed-quantity 9}
        (let [[consumption-uuid resource]
              (sut/seize #:automaton-simulation-de.rc{:capacity 9} 9 {:a :b})]
          (get-in resource [::sim-rc/currently-consuming consumption-uuid]))))
    (is (= #:automaton-simulation-de.rc{:seizing-event {:a :b}
                                        :consumed-quantity 1}
           (let [[consumption-uuid resource]
                 (sut/seize #:automaton-simulation-de.rc{} 1 {:a :b})]
             (get-in resource
                     [::sim-rc/currently-consuming consumption-uuid])))))
  (testing "Seizing one resource with capacity missing"
    (is (nil? (first (sut/seize #:automaton-simulation-de.rc{:capacity 0}
                                1
                                {:a :b}))))
    (is (nil? (first (sut/seize #:automaton-simulation-de.rc{:capacity 12}
                                20
                                {:a :b}))))
    (is (empty?
         (-> (sut/seize #:automaton-simulation-de.rc{:capacity 12} 20 {:a :b})
             second
             ::sim-rc/currently-consuming)))
    (is
     (=
      #:automaton-simulation-de.rc{:capacity 12
                                   :queue
                                   [#:automaton-simulation-de.rc{:seizing-event
                                                                 {:a :b}
                                                                 :consumed-quantity
                                                                 20}]}
      (-> (sut/seize #:automaton-simulation-de.rc{:capacity 12} 20 {:a :b})
          second)))
    (is (-> (sut/seize #:automaton-simulation-de.rc{:capacity 12} 20 {:a :b})
            first
            nil?))))

(deftest dispose-test
  (testing "Dispose a non existing resource is noop"
    (is (= [[]
            #:automaton-simulation-de.rc{:currently-consuming {}
                                         :queue []}]
           (dispose-cacheproof {} 1))))
  (testing "Dispose an existing event, no operation is pending"
    (is
     (=
      [[]
       #:automaton-simulation-de.rc{:capacity 20
                                    :currently-consuming {}
                                    :queue []}]
      (dispose-cacheproof
       #:automaton-simulation-de.rc{:capacity 20
                                    :currently-consuming
                                    {:aa
                                     #:automaton-simulation-de.rc{:consumed-quantity
                                                                  13}}
                                    :queue []}
       :aa))))
  (testing "Dispose an existing event, an operation is pending"
    (is
     (=
      [[#:automaton-simulation-de.rc{:seizing-event {:a :b}
                                     :consumed-quantity 12}]
       #:automaton-simulation-de.rc{:capacity 20
                                    :currently-consuming {}
                                    :queue []}]
      (dispose-cacheproof
       #:automaton-simulation-de.rc{:capacity 20
                                    :currently-consuming
                                    {:aa
                                     #:automaton-simulation-de.rc{:consumed-quantity
                                                                  13}}
                                    :queue
                                    [#:automaton-simulation-de.rc{:seizing-event
                                                                  {:a :b}
                                                                  :consumed-quantity
                                                                  12}]}
       :aa)))))

(deftest dispose-seize-test
  (testing "Disposing 1 which is not enough for next event"
    (let [resource (-> #:automaton-simulation-de.rc{:capacity 1}
                       (sut/seize 1 {:a :b1})
                       second
                       (sut/seize 2 {:a :b2})
                       second
                       (sut/seize 3 {:a :b3})
                       second)
          consumption-uuid (-> resource
                               ::sim-rc/currently-consuming
                               ffirst)]
      (is
       (=
        [[]
         #:automaton-simulation-de.rc{:currently-consuming {}
                                      :capacity 1
                                      :queue
                                      [#:automaton-simulation-de.rc{:seizing-event
                                                                    {:a :b2}
                                                                    :consumed-quantity
                                                                    2}
                                       #:automaton-simulation-de.rc{:seizing-event
                                                                    {:a :b3}
                                                                    :consumed-quantity
                                                                    3}]}]
        (dispose-cacheproof resource consumption-uuid)))))
  (testing
    "There is 1 capacity left, and 1 disposed, which is enough for the next one"
    (let [resource (-> #:automaton-simulation-de.rc{:capacity 2}
                       (sut/seize 1 {:a :b1})
                       second
                       (sut/seize 2 {:a :b2})
                       second
                       (sut/seize 3 {:a :b3})
                       second)
          consumption-uuid (-> resource
                               ::sim-rc/currently-consuming
                               ffirst)]
      (is
       (=
        [[#:automaton-simulation-de.rc{:seizing-event {:a :b2}
                                       :consumed-quantity 2}]
         #:automaton-simulation-de.rc{:currently-consuming {}
                                      :capacity 2
                                      :queue
                                      [#:automaton-simulation-de.rc{:seizing-event
                                                                    {:a :b3}
                                                                    :consumed-quantity
                                                                    3}]}]
        (dispose-cacheproof resource consumption-uuid))))))

(deftest update-capacity-test
  (testing "Empty resource is updated to 7"
    (is (= [[]
            #:automaton-simulation-de.rc{:capacity 7
                                         :queue []}]
           (update-capacity-cacheproof {} 7)
           (update-capacity-cacheproof nil 7)
           (update-capacity-cacheproof #:automaton-simulation-de.rc{:capacity 5}
                                       7))))
  (testing "A resource is updated with the `new-capacity`"
    (is (= 14
           (-> #:automaton-simulation-de.rc{:capacity 1
                                            :preemption-policy
                                            ::sim-rc/no-preemption}
               (update-capacity-cacheproof 14)
               second
               ::sim-rc/capacity))))
  (testing
    "Not implemented preemption policy is raising an error, if the capacity is decreased"
    (is (-> #:automaton-simulation-de.rc{:capacity 1
                                         :preemption-policy
                                         ::sim-rc/not-existing-policy}
            (update-capacity-cacheproof 12)))
    (is (-> #:automaton-simulation-de.rc{:capacity 5
                                         :preemption-policy
                                         ::sim-rc/not-existing-policy}
            (update-capacity-cacheproof 5))))
  (testing "Preemption is defaulted to no-preemption"
    (is (= [[]
            #:automaton-simulation-de.rc{:capacity 12
                                         :queue []}]
           (update-capacity-cacheproof #:automaton-simulation-de.rc{:capacity 5}
                                       12)))))
