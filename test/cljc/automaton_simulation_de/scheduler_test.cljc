(ns automaton-simulation-de.scheduler-test
  (:require
   #?(:clj [clojure.test :refer [deftest is testing]]
      :cljs [cljs.test :refer [deftest is testing] :include-macros true])
   [automaton-core.utils.map :as utils-map]
   [automaton-simulation-de.event :as sim-de-event]
   [automaton-simulation-de.scheduler :as sut]
   [automaton-simulation-de.scheduler.middleware
    :as
    sim-de-scheduler-middleware]))

;; Problem data
(def routing
  {:m1 [:m2]
   :m2 [:m4]
   :m3 [:m4]})

(def process-time
  {:m1 1
   :m2 3
   :m3 3
   :m4 1})

(def transportation-duration 2)

;; Events
(defn evt-init
  "Add all products arrival to machine M1"
  [_state
   future-events
   {:keys [date]
    :as _e}]
  {:state {}
   :future-events (-> future-events
                      (concat [{:type :MA
                                :date date
                                :product :p1
                                :machine :m1}
                               {:type :MA
                                :date date
                                :product :p2
                                :machine :m1}
                               {:type :MA
                                :date date
                                :product :p3
                                :machine :m1}]))})

(defn machine-arrive
  "Product `p` is added on machine `m` input buffer at date `d`
  Creates a new event machine start for the same product `p` starts on machine `m` at date `d`"
  [state
   future-events
   {:keys [date product machine]
    :as _e}]
  (let [state (-> state
                  (update-in [machine :input]
                             (fn [list-products]
                               (vec (conj list-products product)))))
        future-events (->> future-events
                           (cons {:type :MP
                                  :date date
                                  :product product
                                  :machine machine}))]
    {:state state
     :future-events future-events}))

(defn machine-process
  [state
   future-events
   {:keys [date product machine]
    :as _e}]
  (let [state (-> state
                  (update-in [machine :input]
                             (fn [list-products]
                               (vec (remove #{product} list-products))))
                  (assoc-in [machine :process] product))
        new-date (+ date (process-time machine))
        future-events (-> future-events
                          (conj {:type :MT
                                 :date new-date
                                 :product product
                                 :machine machine})
                          (sim-de-event/postpone-events
                           (fn [{:keys [type machine]}]
                             (and (= machine machine) (= type :MP)))
                           new-date))]
    {:state state
     :future-events future-events}))

(defn machine-terminate
  [state
   future-events
   {:keys [date product machine]
    :as _e}]
  (let [state (assoc-in state [machine :process] nil)
        transportation-end-time (+ date transportation-duration)
        future-events (if-let [next-m (rand-nth (get routing machine))]
                        (cons {:type :MA
                               :date transportation-end-time
                               :product product
                               :machine next-m}
                              future-events)
                        (cons {:type :PT
                               :date transportation-end-time
                               :product product}
                              future-events))]
    {:state state
     :future-events future-events}))

(defn part-terminate
  [state future-events & _]
  {:state state
   :future-events future-events})


(def ^:private evt-registry-kvs
  {:IN evt-init
   :MA machine-arrive
   :MP machine-process
   :MT machine-terminate
   :PT part-terminate})

(defn same-date-ordering
  [e1 e2]
  (let [evt-type-priority [:IN :MA :PT :MT :MP]]
    (cond
      (not= (:type e1) (:type e2)) (< (.indexOf evt-type-priority (:type e1))
                                      (.indexOf evt-type-priority (:type e2)))
      (not= (:product e1) (:product e2)) (neg? (compare (:product e1)
                                                        (:product e2)))
      (not= (:machine e1) (:machine e2)) (neg? (compare (:machine e1)
                                                        (:machine e2))))))

(def scheduler-first-iteration
  {:id 0
   :state {}
   :past-events []
   :future-events [{:type :IN
                    :date 0}]})
(def evt-registry
  {:event-registry-kvs evt-registry-kvs
   :event-ordering {:same-date-ordering same-date-ordering}})

(deftest scheduler-test
  (testing "scheduler returns expected iteration"
    (let [{:keys [id state past-events future-events]}
          (sut/scheduler evt-registry scheduler-first-iteration [])]
      (is (= 31 id))
      (is
       (=
        [{:type :PT
          :date 13
          :product :p3}
         {:type :PT
          :date 12
          :product :p2}
         {:type :MT
          :date 11
          :product :p3
          :machine :m4}
         {:type :PT
          :date 11
          :product :p1}
         {:type :MP
          :date 10
          :product :p3
          :machine :m4}
         {:type :MT
          :date 10
          :product :p2
          :machine :m4}
         {:type :MA
          :date 10
          :product :p3
          :machine :m4}
         {:type :MP
          :date 9
          :product :p2
          :machine :m4}
         {:type :MT
          :date 9
          :product :p1
          :machine :m4}
         {:type :MA
          :date 9
          :product :p2
          :machine :m4}
         {:type :MP
          :date 8
          :product :p1
          :machine :m4}
         {:type :MT
          :date 8
          :product :p3
          :machine :m2}
         {:type :MA
          :date 8
          :product :p1
          :machine :m4}
         {:type :MT
          :date 7
          :product :p2
          :machine :m2}
         {:type :MT
          :date 6
          :product :p1
          :machine :m2}
         {:type :MP
          :date 5
          :product :p3
          :machine :m2}
         {:type :MA
          :date 5
          :product :p3
          :machine :m2}
         {:type :MP
          :date 4
          :product :p2
          :machine :m2}
         {:type :MA
          :date 4
          :product :p2
          :machine :m2}
         {:type :MP
          :date 3
          :product :p1
          :machine :m2}
         {:type :MT
          :date 3
          :product :p3
          :machine :m1}
         {:type :MA
          :date 3
          :product :p1
          :machine :m2}
         {:type :MP
          :date 2
          :product :p3
          :machine :m1}
         {:type :MT
          :date 2
          :product :p2
          :machine :m1}
         {:type :MP
          :date 1
          :product :p2
          :machine :m1}
         {:type :MT
          :date 1
          :product :p1
          :machine :m1}
         {:type :MP
          :date 0
          :product :p1
          :machine :m1}
         {:type :MA
          :date 0
          :product :p3
          :machine :m1}
         {:type :MA
          :date 0
          :product :p2
          :machine :m1}
         {:type :MA
          :date 0
          :product :p1
          :machine :m1}
         {:type :IN
          :date 0}]
        past-events))
      (is (empty? (utils-map/map-difference state
                                            {:m1 {:input []
                                                  :process nil}
                                             :m2 {:input []
                                                  :process nil}
                                             :m4 {:input []
                                                  :process nil}
                                             :stop true})))
      (is (= (count past-events) 31))
      (is (empty? future-events))))
  (testing "scheduler stops when stopping criteria is added"
    (is (= 5
           (:id (sut/scheduler evt-registry
                               scheduler-first-iteration
                               [#(if (> (:id %) 4)
                                   (sim-de-scheduler-middleware/stop-iteration
                                    %)
                                   %)]))))))
