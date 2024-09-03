(ns automaton-simulation-de.transformation-test
  (:require
   [automaton-simulation-de.entity            :as sim-entity]
   [automaton-simulation-de.predicates        :as sim-pred]
   [automaton-simulation-de.rc                :as-alias sim-rc]
   [automaton-simulation-de.simulation-engine :as-alias sim-engine]
   #?(:clj [clojure.test :refer [deftest is testing]]
      :cljs [cljs.test :refer [deftest is testing] :include-macros true])
   [automaton-simulation-de.transformation    :as sut]))

(deftest snapshot-use-cases
  (testing "state filtering"
    (is (= {:m1 {:id :m1}}
           (-> [::sim-pred/equal? :id :m1]
               sim-pred/predicate-lang->predicate-fn
               (sut/keep-state {:m1 {:id :m1}
                                :m2 {:id :m2}
                                :m3 {:id :m3}
                                :m4 {:id :m4}})))
        "state contains only :m1")
    (is (= {:m2 {:id :m2
                 :processing :p1}
            :m4 {:id :m4
                 :processing :p2}}
           (-> [::sim-pred/one-of? :processing [:p1 :p2]]
               sim-pred/predicate-lang->predicate-fn
               (sut/keep-state {:m1 {:id :m1
                                     :processing :p3}
                                :m2 {:id :m2
                                     :processing :p1}
                                :m3 {:id :m3}
                                :m4 {:id :m4
                                     :processing :p2}})))
        "state contains only machines having :p1 or :p2 under :processing")
    (is (= {:m2 {:id :m2
                 :name "MX5009"}
            :m3 {:id :m3
                 :name "MX-TURBO-5892"}}
           (-> [::sim-pred/starts-with? :name "MX"]
               sim-pred/predicate-lang->predicate-fn
               (sut/keep-state {:m1 {:id :m1
                                     :name "BR500"}
                                :m2 {:id :m2
                                     :name "MX5009"}
                                :m3 {:id :m3
                                     :name "MX-TURBO-5892"}
                                :m4 {:id :m4
                                     :name "MA4"
                                     :processing :p2}})))
        "state contains only machines starting with name MX"))
  (testing "events filtering"
    (is (= [{:product :p1
             :first :me}
            {:product :p1
             :second :me}]
           (-> [::sim-pred/equal? :product :p1]
               sim-pred/predicate-lang->predicate-fn
               (sut/keep-events [{:product :p1
                                  :first :me}
                                 {:product :p2}
                                 {:product :p1
                                  :second :me}
                                 {:whatever :other}
                                 {:more "whatever"}])))
        "past events that are regarding a product"))
  (testing "snapshot state filtering"
    (is
     (=
      {::sim-engine/state {:m2 {:id :m2
                                :processing :p1}
                           :m4 {:id :m4
                                :processing :p2}}
       ::sim-engine/date 0
       ::sim-engine/past-events [{:product :p1
                                  :first :me
                                  :machine :m1}
                                 {:product :p2
                                  :machine :m1}
                                 {:product :p1
                                  :second :me
                                  :machine :m2}
                                 {:whatever :other
                                  :machine :m4}
                                 {:more "whatever"
                                  :machine :m1}]
       ::sim-engine/future-events [{:product :p1
                                    :machine :m3}
                                   {:product :p1
                                    :machine :m4}]}
      (-> [::sim-pred/one-of? :processing [:p1 :p2]]
          sim-pred/predicate-lang->predicate-fn
          (sut/keep-snapshot-state
           {::sim-engine/state {:m1 {:id :m1
                                     :processing :p3}
                                :m2 {:id :m2
                                     :processing :p1}
                                :m3 {:id :m3}
                                :m4 {:id :m4
                                     :processing :p2}}
            ::sim-engine/date 0
            ::sim-engine/past-events [{:product :p1
                                       :first :me
                                       :machine :m1}
                                      {:product :p2
                                       :machine :m1}
                                      {:product :p1
                                       :second :me
                                       :machine :m2}
                                      {:whatever :other
                                       :machine :m4}
                                      {:more "whatever"
                                       :machine :m1}]
            ::sim-engine/future-events [{:product :p1
                                         :machine :m3}
                                        {:product :p1
                                         :machine :m4}]})))
     "state contains only machines having :p1 or :p2 under :processing"))
  (testing "snapshot based on events filtering"
    (is
     (=
      {::sim-engine/state {:m4 {:id :m4
                                :name "MA4"
                                :processing :p2}}
       ::sim-engine/date 0
       ::sim-engine/past-events [{:whatever :other
                                  :machine :m4}]
       ::sim-engine/future-events [{:product :p1
                                    :machine :m4}]}
      (-> [::sim-pred/not [::sim-pred/is-empty? :processing]]
          sim-pred/predicate-lang->predicate-fn
          (sut/keep-snapshot-events-based-state :machine
                                                {::sim-engine/state {:m1 {:id :m1
                                                                          :name "BR500"}
                                                                     :m2 {:id :m2
                                                                          :name "MX5009"}
                                                                     :m3 {:id :m3
                                                                          :name "MX-TURBO-5892"}
                                                                     :m4 {:id :m4
                                                                          :name "MA4"
                                                                          :processing :p2}}
                                                 ::sim-engine/date 0
                                                 ::sim-engine/past-events [{:product :p1
                                                                            :first :me
                                                                            :machine :m1}
                                                                           {:product :p2
                                                                            :machine :m1}
                                                                           {:product :p1
                                                                            :second :me
                                                                            :machine :m2}
                                                                           {:whatever :other
                                                                            :machine :m4}
                                                                           {:more "whatever"
                                                                            :machine :m1}]
                                                 ::sim-engine/future-events [{:product :p1
                                                                              :machine :m3}
                                                                             {:product :p1
                                                                              :machine :m4}]})))
     "Filter state to only consist of machines that have somethin in :processing in their state and filter all events regarding them")
    (is
     (=
      {::sim-engine/state {:m1 {:id :m1
                                :name "BR500"
                                :color "blue"}
                           :m3 {:id :m3
                                :name "MX-TURBO-5892"
                                :color "blue"}}
       ::sim-engine/date 0
       ::sim-engine/past-events [{:important true
                                  :machine :m1}]
       ::sim-engine/future-events [{:product :p1
                                    :machine :m3
                                    :important true}]}
      (-> [::sim-pred/or [::sim-pred/equal? :color "blue"] [::sim-pred/true? :important]]
          sim-pred/predicate-lang->predicate-fn
          (sut/keep-snapshot
           {::sim-engine/state {:m1 {:id :m1
                                     :name "BR500"
                                     :color "blue"}
                                :m2 {:id :m2
                                     :name "MX5009"}
                                :m3 {:id :m3
                                     :name "MX-TURBO-5892"
                                     :color "blue"}
                                :m4 {:id :m4
                                     :name "MA4"
                                     :processing :p2}}
            ::sim-engine/date 0
            ::sim-engine/past-events [{:product :p1
                                       :first :me
                                       :machine :m1}
                                      {:product :p2
                                       :machine :m1}
                                      {:product :p1
                                       :second :me
                                       :machine :m2}
                                      {:whatever :other
                                       :machine :m4}
                                      {:important true
                                       :machine :m1}]
            ::sim-engine/future-events [{:product :p1
                                         :machine :m3
                                         :important true}
                                        {:product :p1
                                         :machine :m4}]})))
     "Filter state machines that has :blue color or :important true and filter all events regarding them")))

(deftest stopping-causes
  (is
   (=
    [{::sim-engine/context {::sim-engine/iteration 1
                            ::sim-engine/n 1}
      ::sim-engine/stopping-criteria {::sim-engine/params {::sim-engine/n 1}
                                      ::sim-engine/stopping-definition
                                      {::sim-engine/doc "Stops when the iteration `n` is reached."
                                       ::sim-engine/id ::sim-engine/iteration-nth
                                       ::sim-engine/next-possible? true
                                       ::sim-engine/stopping-evaluation nil}
                                      ::sim-engine/model-end? false}
      ::sim-engine/current-event {::sim-engine/type :IN
                                  ::sim-engine/date 0}}]
    (-> [::sim-pred/equal? [::sim-engine/id] ::sim-engine/iteration-nth]
        sim-pred/predicate-lang->predicate-fn
        (sut/keep-stopping-causes-by-stopping-definition
         [{::sim-engine/stopping-criteria {::sim-engine/stopping-definition
                                           {::sim-engine/id ::sim-engine/no-future-events
                                            ::sim-engine/next-possible? false
                                            ::sim-engine/doc
                                            "Stops when no future events exists anymore."}}
           ::sim-engine/current-event nil}
          {::sim-engine/context {::sim-engine/iteration 1
                                 ::sim-engine/n 1}
           ::sim-engine/stopping-criteria {::sim-engine/params {::sim-engine/n 1}
                                           ::sim-engine/stopping-definition
                                           {::sim-engine/doc
                                            "Stops when the iteration `n` is reached."
                                            ::sim-engine/id ::sim-engine/iteration-nth
                                            ::sim-engine/next-possible? true
                                            ::sim-engine/stopping-evaluation nil}
                                           ::sim-engine/model-end? false}
           ::sim-engine/current-event {::sim-engine/type :IN
                                       ::sim-engine/date 0}}])))
   "stopping causes are filtered to contain only iteration-nth stopping-cause")
  (is (= []
         (-> [::sim-pred/one-of? :processing [:p1 :p2]]
             sim-pred/predicate-lang->predicate-fn
             (sut/keep-stopping-causes-by-model-end
              [{::sim-engine/stopping-criteria {::sim-engine/stopping-definition
                                                {::sim-engine/id ::sim-engine/no-future-events
                                                 ::sim-engine/next-possible? false
                                                 ::sim-engine/doc
                                                 "Stops when no future events exists anymore."}}
                ::sim-engine/current-event nil}
               {::sim-engine/context {::sim-engine/iteration 1
                                      ::sim-engine/n 1}
                ::sim-engine/stopping-criteria {::sim-engine/params {::sim-engine/n 1}
                                                ::sim-engine/stopping-definition
                                                {::sim-engine/doc
                                                 "Stops when the iteration `n` is reached."
                                                 ::sim-engine/id ::sim-engine/iteration-nth
                                                 ::sim-engine/next-possible? true
                                                 ::sim-engine/stopping-evaluation nil}
                                                ::sim-engine/model-end? false}
                ::sim-engine/current-event {::sim-engine/type :IN
                                            ::sim-engine/date 0}}])))
      "No model-end returns empty array")
  (is (= [{::sim-engine/stopping-criteria {::sim-engine/stopping-definition
                                           {::sim-engine/id ::sim-engine/no-future-events
                                            ::sim-engine/next-possible? false
                                            ::sim-engine/doc
                                            "Stops when no future events exists anymore."}
                                           ::sim-engine/model-end? true}
           ::sim-engine/current-event nil}]
         (-> [::sim-pred/true?]
             sim-pred/predicate-lang->predicate-fn
             (sut/keep-stopping-causes-by-model-end
              [{::sim-engine/stopping-criteria {::sim-engine/stopping-definition
                                                {::sim-engine/id ::sim-engine/no-future-events
                                                 ::sim-engine/next-possible? false
                                                 ::sim-engine/doc
                                                 "Stops when no future events exists anymore."}
                                                ::sim-engine/model-end? true}
                ::sim-engine/current-event nil}
               {::sim-engine/context {::sim-engine/iteration 1
                                      ::sim-engine/n 1}
                ::sim-engine/stopping-criteria {::sim-engine/params {::sim-engine/n 1}
                                                ::sim-engine/stopping-definition
                                                {::sim-engine/doc
                                                 "Stops when the iteration `n` is reached."
                                                 ::sim-engine/id ::sim-engine/iteration-nth
                                                 ::sim-engine/next-possible? true
                                                 ::sim-engine/stopping-evaluation nil}
                                                ::sim-engine/model-end? false}
                ::sim-engine/current-event {::sim-engine/type :IN
                                            ::sim-engine/date 0}}])))
      "Model end stopping-criteria is filtered"))



(deftest multiple-snapshots-use-cases
  (testing
    "I have multiple snapshots, I want to keep only those that after applying keep-predicate on state it's different from previous filtered snapshot under state"
    (is
     (=
      [{::sim-engine/state {:m4 {:id :m4
                                 :processing :p2}}
        ::sim-engine/iteration 10
        ::sim-engine/date 0
        ::sim-engine/past-events [{:whatever :other
                                   :machine :m4}]
        ::sim-engine/future-events [{:product :p1
                                     :machine :m4}]}
       {::sim-engine/state {:m4 {:id :m4
                                 :processing nil}}
        ::sim-engine/iteration 12
        ::sim-engine/date 0
        ::sim-engine/past-events [{:whatever :other
                                   :machine :m4}]
        ::sim-engine/future-events [{:product :p1
                                     :machine :m4}]}
       {::sim-engine/state {:m4 {:id :m4
                                 :input [:p1]
                                 :processing nil}}
        ::sim-engine/iteration 14
        ::sim-engine/date 0
        ::sim-engine/past-events [{:whatever :other
                                   :machine :m4}]
        ::sim-engine/future-events [{:product :p1
                                     :machine :m4}]}]
      (-> [::sim-pred/equal? :id :m4]
          sim-pred/predicate-lang->predicate-fn
          (sut/keep-snapshots-state
           [{::sim-engine/state {:m4 {:id :m4
                                      :processing :p2}}
             ::sim-engine/iteration 10
             ::sim-engine/date 0
             ::sim-engine/past-events [{:whatever :other
                                        :machine :m4}]
             ::sim-engine/future-events [{:product :p1
                                          :machine :m4}]}
            {::sim-engine/state {:m4 {:id :m4
                                      :processing :p2}}
             ::sim-engine/iteration 11
             ::sim-engine/date 0
             ::sim-engine/past-events [{:whatever :other
                                        :machine :m4}]
             ::sim-engine/future-events [{:product :p1
                                          :machine :m4}]}
            {::sim-engine/state {:m4 {:id :m4
                                      :processing nil}}
             ::sim-engine/iteration 12
             ::sim-engine/date 0
             ::sim-engine/past-events [{:whatever :other
                                        :machine :m4}]
             ::sim-engine/future-events [{:product :p1
                                          :machine :m4}]}
            {::sim-engine/state {:m4 {:id :m4
                                      :processing nil}}
             ::sim-engine/iteration 13
             ::sim-engine/date 0
             ::sim-engine/past-events [{:whatever :other
                                        :machine :m4}]
             ::sim-engine/future-events [{:product :p1
                                          :machine :m4}]}
            {::sim-engine/state {:m4 {:id :m4
                                      :input [:p1]
                                      :processing nil}}
             ::sim-engine/iteration 14
             ::sim-engine/date 0
             ::sim-engine/past-events [{:whatever :other
                                        :machine :m4}]
             ::sim-engine/future-events [{:product :p1
                                          :machine :m4}]}]))))))

(deftest rc-snapshot-use-cases
  (testing "state filtering"
    (is (= {::sim-rc/resource {:m1 {:id :m1}}}
           (-> [::sim-pred/equal? :id :m1]
               sim-pred/predicate-lang->predicate-fn
               (sut/keep-state-resource {::sim-rc/resource {:m1 {:id :m1}
                                                            :m2 {:id :m2}
                                                            :m3 {:id :m3}
                                                            :m4 {:id :m4}}})))
        "state contains only :m1")
    (is (= {::sim-rc/resource {:m2 {:id :m2
                                    :processing :p1}
                               :m4 {:id :m4
                                    :processing :p2}}}
           (-> [::sim-pred/one-of? :processing [:p1 :p2]]
               sim-pred/predicate-lang->predicate-fn
               (sut/keep-state-resource {::sim-rc/resource {:m1 {:id :m1
                                                                 :processing :p3}
                                                            :m2 {:id :m2
                                                                 :processing :p1}
                                                            :m3 {:id :m3}
                                                            :m4 {:id :m4
                                                                 :processing :p2}}})))
        "state contains only machines having :p1 or :p2 under :processing")
    (is (= {::sim-rc/resource {:m2 {:id :m2
                                    :name "MX5009"}
                               :m3 {:id :m3
                                    :name "MX-TURBO-5892"}}}
           (-> [::sim-pred/starts-with? :name "MX"]
               sim-pred/predicate-lang->predicate-fn
               (sut/keep-state-resource {::sim-rc/resource {:m1 {:id :m1
                                                                 :name "BR500"}
                                                            :m2 {:id :m2
                                                                 :name "MX5009"}
                                                            :m3 {:id :m3
                                                                 :name "MX-TURBO-5892"}
                                                            :m4 {:id :m4
                                                                 :name "MA4"
                                                                 :processing :p2}}})))
        "state contains only machines starting with name MX"))
  (testing "snapshot state filtering"
    (is
     (=
      {::sim-engine/state {::sim-rc/resource {:m2 {:id :m2
                                                   :processing :p1}
                                              :m4 {:id :m4
                                                   :processing :p2}}}
       ::sim-engine/date 0
       ::sim-engine/past-events [{:product :p1
                                  :first :me
                                  :machine :m1}
                                 {:product :p2
                                  :machine :m1}
                                 {:product :p1
                                  :second :me
                                  :machine :m2}
                                 {:whatever :other
                                  :machine :m4}
                                 {:more "whatever"
                                  :machine :m1}]
       ::sim-engine/future-events [{:product :p1
                                    :machine :m3}
                                   {:product :p1
                                    :machine :m4}]}
      (-> [::sim-pred/one-of? :processing [:p1 :p2]]
          sim-pred/predicate-lang->predicate-fn
          (sut/keep-snapshot-state-resource
           {::sim-engine/state {::sim-rc/resource {:m1 {:id :m1
                                                        :processing :p3}
                                                   :m2 {:id :m2
                                                        :processing :p1}
                                                   :m3 {:id :m3}
                                                   :m4 {:id :m4
                                                        :processing :p2}}}
            ::sim-engine/date 0
            ::sim-engine/past-events [{:product :p1
                                       :first :me
                                       :machine :m1}
                                      {:product :p2
                                       :machine :m1}
                                      {:product :p1
                                       :second :me
                                       :machine :m2}
                                      {:whatever :other
                                       :machine :m4}
                                      {:more "whatever"
                                       :machine :m1}]
            ::sim-engine/future-events [{:product :p1
                                         :machine :m3}
                                        {:product :p1
                                         :machine :m4}]})))
     "state contains only machines having :p1 or :p2 under :processing"))
  (testing "snapshot based on events filtering"
    (is
     (=
      {::sim-engine/state {::sim-rc/resource {:m4 {:id :m4
                                                   :name "MA4"
                                                   :processing :p2}}}
       ::sim-engine/date 0
       ::sim-engine/past-events [{:whatever :other
                                  :machine :m4}]
       ::sim-engine/future-events [{:product :p1
                                    :machine :m4}]}
      (-> [::sim-pred/not [::sim-pred/is-empty? :processing]]
          sim-pred/predicate-lang->predicate-fn
          (sut/keep-snapshot-events-based-state-resource
           :machine
           {::sim-engine/state {::sim-rc/resource {:m1 {:id :m1
                                                        :name "BR500"}
                                                   :m2 {:id :m2
                                                        :name "MX5009"}
                                                   :m3 {:id :m3
                                                        :name "MX-TURBO-5892"}
                                                   :m4 {:id :m4
                                                        :name "MA4"
                                                        :processing :p2}}}
            ::sim-engine/date 0
            ::sim-engine/past-events [{:product :p1
                                       :first :me
                                       :machine :m1}
                                      {:product :p2
                                       :machine :m1}
                                      {:product :p1
                                       :second :me
                                       :machine :m2}
                                      {:whatever :other
                                       :machine :m4}
                                      {:more "whatever"
                                       :machine :m1}]
            ::sim-engine/future-events [{:product :p1
                                         :machine :m3}
                                        {:product :p1
                                         :machine :m4}]})))
     "Filter state to only consist of machines that have somethin in :processing in their state and filter all events regarding them")
    (is
     (=
      {::sim-engine/state {::sim-rc/resource {:m1 {:id :m1
                                                   :name "BR500"
                                                   :color "blue"}
                                              :m3 {:id :m3
                                                   :name "MX-TURBO-5892"
                                                   :color "blue"}}}
       ::sim-engine/date 0
       ::sim-engine/past-events [{:important true
                                  :machine :m1}]
       ::sim-engine/future-events [{:product :p1
                                    :machine :m3
                                    :important true}]}
      (-> [::sim-pred/or [::sim-pred/equal? :color "blue"] [::sim-pred/true? :important]]
          sim-pred/predicate-lang->predicate-fn
          (sut/keep-snapshot-resource
           {::sim-engine/state {::sim-rc/resource {:m1 {:id :m1
                                                        :name "BR500"
                                                        :color "blue"}
                                                   :m2 {:id :m2
                                                        :name "MX5009"}
                                                   :m3 {:id :m3
                                                        :name "MX-TURBO-5892"
                                                        :color "blue"}
                                                   :m4 {:id :m4
                                                        :name "MA4"
                                                        :processing :p2}}}
            ::sim-engine/date 0
            ::sim-engine/past-events [{:product :p1
                                       :first :me
                                       :machine :m1}
                                      {:product :p2
                                       :machine :m1}
                                      {:product :p1
                                       :second :me
                                       :machine :m2}
                                      {:whatever :other
                                       :machine :m4}
                                      {:important true
                                       :machine :m1}]
            ::sim-engine/future-events [{:product :p1
                                         :machine :m3
                                         :important true}
                                        {:product :p1
                                         :machine :m4}]})))
     "Filter state machines that has :blue color or :important true and filter all events regarding them")))

(deftest entity-snapshot-use-cases
  (testing "state filtering"
    (is (= (sim-entity/create {}
                              3
                              :p1
                              {:data :of
                               :an :entity
                               :id :p1})
           (-> [::sim-pred/equal? :id :p1]
               sim-pred/predicate-lang->predicate-fn
               (sut/keep-state-entity
                (-> {}
                    (sim-entity/create 3
                                       :p1
                                       {:data :of
                                        :an :entity
                                        :id :p1})
                    (sim-entity/create 5
                                       :p2
                                       {:data :of
                                        :an :entity
                                        :id :p2})
                    (sim-entity/create 7
                                       :p3
                                       {:data :of
                                        :an :entity
                                        :id :p3})
                    (sim-entity/create 8
                                       :p4
                                       {:data :of
                                        :an :entity
                                        :id :p4})))))
        "state contains only :m1")
    (is
     (=
      (-> {}
          (sim-entity/create 3
                             :p1
                             {:data :of
                              :an :entity
                              :color :green
                              :id :p1})
          (sim-entity/create 3
                             :p3
                             {:data :of
                              :color :blue
                              :an :entity
                              :id :p3}))
      (-> [::sim-pred/one-of? :color [:blue :green]]
          sim-pred/predicate-lang->predicate-fn
          (sut/keep-state-entity
           (-> {}
               (sim-entity/create 3
                                  :p1
                                  {:data :of
                                   :an :entity
                                   :color :green
                                   :id :p1})
               (sim-entity/create 3
                                  :p2
                                  {:data :of
                                   :color :red
                                   :an :entity
                                   :id :p2})
               (sim-entity/create 3
                                  :p3
                                  {:data :of
                                   :color :blue
                                   :an :entity
                                   :id :p3})
               (sim-entity/create 3
                                  :p4
                                  {:data :of
                                   :color :yellow
                                   :an :entity
                                   :id :p2})))))
     "state contains only products having :blue or :green under :color data")
    (is
     (=
      (-> {}
          (sim-entity/create 3
                             :p3
                             {:data :of
                              :an :entity
                              :name "MX5009"
                              :id :p3})
          (sim-entity/create 3
                             :p4
                             {:data :of
                              :name "MX-TURBO-5892"
                              :an :entity
                              :id :p4}))
      (-> [::sim-pred/starts-with? :name "MX"]
          sim-pred/predicate-lang->predicate-fn
          (sut/keep-state-entity
           (-> {}
               (sim-entity/create 3
                                  :p1
                                  {:data :of
                                   :an :entity
                                   :name "BR500"
                                   :id :p1})
               (sim-entity/create 3
                                  :p2
                                  {:data :of
                                   :an :entity
                                   :id :p2})
               (sim-entity/create 3
                                  :p3
                                  {:data :of
                                   :an :entity
                                   :name "MX5009"
                                   :id :p3})
               (sim-entity/create 3
                                  :p4
                                  {:data :of
                                   :name "MX-TURBO-5892"
                                   :an :entity
                                   :id :p4})))))
     "state contains only products starting with name MX"))
  (testing "snapshot state filtering"
    (is
     (=
      {::sim-engine/state (-> {}
                              (sim-entity/create 3
                                                 :p3
                                                 {:data :of
                                                  :an :entity
                                                  :name "MX5009"
                                                  :id :p3})
                              (sim-entity/create 3
                                                 :p4
                                                 {:data :of
                                                  :name "MX-TURBO-5892"
                                                  :an :entity
                                                  :id :p4}))
       ::sim-engine/date 0
       ::sim-engine/past-events [{:product :p1
                                  :first :me
                                  :machine :m1}
                                 {:product :p2
                                  :machine :m1}
                                 {:product :p1
                                  :second :me
                                  :machine :m2}
                                 {:whatever :other
                                  :machine :m4}
                                 {:more "whatever"
                                  :machine :m1}]
       ::sim-engine/future-events [{:product :p1
                                    :machine :m3}
                                   {:product :p1
                                    :machine :m4}]}
      (-> [::sim-pred/one-of? :id [:p3 :p4]]
          sim-pred/predicate-lang->predicate-fn
          (sut/keep-snapshot-state-entity
           {::sim-engine/state (-> {}
                                   (sim-entity/create 3
                                                      :p1
                                                      {:data :of
                                                       :an :entity
                                                       :name "BR500"
                                                       :id :p1})
                                   (sim-entity/create 3
                                                      :p2
                                                      {:data :of
                                                       :an :entity
                                                       :id :p2})
                                   (sim-entity/create 3
                                                      :p3
                                                      {:data :of
                                                       :an :entity
                                                       :name "MX5009"
                                                       :id :p3})
                                   (sim-entity/create 3
                                                      :p4
                                                      {:data :of
                                                       :name "MX-TURBO-5892"
                                                       :an :entity
                                                       :id :p4}))
            ::sim-engine/date 0
            ::sim-engine/past-events [{:product :p1
                                       :first :me
                                       :machine :m1}
                                      {:product :p2
                                       :machine :m1}
                                      {:product :p1
                                       :second :me
                                       :machine :m2}
                                      {:whatever :other
                                       :machine :m4}
                                      {:more "whatever"
                                       :machine :m1}]
            ::sim-engine/future-events [{:product :p1
                                         :machine :m3}
                                        {:product :p1
                                         :machine :m4}]})))
     "state contains only machines having :p1 or :p2 under :processing"))
  (testing "snapshot based on events filtering"
    (is
     (=
      {::sim-engine/state (sim-entity/create {}
                                             3
                                             :p1
                                             {:data :of
                                              :an :entity
                                              :name "BR500"
                                              :id :p1})
       ::sim-engine/date 0
       ::sim-engine/past-events [{:product :p1
                                  :first :me
                                  :machine :m1}
                                 {:product :p1
                                  :second :me
                                  :machine :m2}]
       ::sim-engine/future-events [{:product :p1
                                    :machine :m3}
                                   {:product :p1
                                    :machine :m4}]}
      (-> [::sim-pred/equal? :name "BR500"]
          sim-pred/predicate-lang->predicate-fn
          (sut/keep-snapshot-events-based-state-entity
           :product
           {::sim-engine/state (-> {}
                                   (sim-entity/create 3
                                                      :p1
                                                      {:data :of
                                                       :an :entity
                                                       :name "BR500"
                                                       :id :p1})
                                   (sim-entity/create 3
                                                      :p2
                                                      {:data :of
                                                       :an :entity
                                                       :id :p2})
                                   (sim-entity/create 3
                                                      :p3
                                                      {:data :of
                                                       :an :entity
                                                       :name "MX5009"
                                                       :id :p3})
                                   (sim-entity/create 3
                                                      :p4
                                                      {:data :of
                                                       :name "MX-TURBO-5892"
                                                       :an :entity
                                                       :id :p4}))
            ::sim-engine/date 0
            ::sim-engine/past-events [{:product :p1
                                       :first :me
                                       :machine :m1}
                                      {:product :p2
                                       :machine :m1}
                                      {:product :p1
                                       :second :me
                                       :machine :m2}
                                      {:whatever :other
                                       :machine :m4}
                                      {:more "whatever"
                                       :machine :m1}]
            ::sim-engine/future-events [{:product :p1
                                         :machine :m3}
                                        {:product :p1
                                         :machine :m4}]})))
     "Filter state to only consist of machines that have somethin in :processing in their state and filter all events regarding them")))
