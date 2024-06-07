#_{:heph-ignore {:forbidden-words ["tap>"]}}
(ns automaton-simulation-de.demo.step-1-scheduler
  "Simulation domain, that can be tested from console
   Used to help during development of simulation de concepts"
  (:require
   [automaton-simulation-de.core                   :as simulation-core]
   [automaton-simulation-de.scheduler.event        :as sim-de-event]
   [automaton-simulation-de.scheduler.event-return :as sim-de-event-return]
   [clojure.string                                 :as str]))

;; Problem data
(def routing
  {:m1 [:m2 :m3]
   :m2 [:m4]
   :m3 [:m4]})

(def process-time
  {:m1 1
   :m2 3
   :m3 3
   :m4 1})

;; Events
(defn evt-init
  "Add all products arrival to machine M1"
  [{:keys [::sim-de-event/date]} _state future-events]
  (sim-de-event-return/build {}
                             (-> future-events
                                 (concat [{::sim-de-event/type :MA
                                           ::sim-de-event/date date
                                           ::product :p1
                                           ::machine :m1}
                                          {::sim-de-event/type :MA
                                           ::sim-de-event/date date
                                           ::product :p2
                                           ::machine :m1}
                                          {::sim-de-event/type :MA
                                           ::sim-de-event/date date
                                           ::product :p3
                                           ::machine :m1}]))))

(defn machine-arrive
  "Product `p` is added on machine `m` input buffer at date `d`
  Creates a new event machine start for the same product `p` starts on machine `m` at date `d`"
  [{:keys [::sim-de-event/date ::product ::machine]} state future-events]
  (sim-de-event-return/build
   (-> state
       (update-in [machine :input]
                  (fn [list-products] (vec (conj list-products product)))))
   (->> future-events
        (cons {::sim-de-event/type :MP
               ::sim-de-event/date date
               ::product product
               ::machine machine}))))

(defn machine-process
  [{:keys [::sim-de-event/date ::product ::machine]} state future-events]
  (let [new-date (+ date (process-time machine))]
    (sim-de-event-return/build (-> state
                                   (update-in [machine :input]
                                              (fn [list-products]
                                                (vec (remove #{product}
                                                             list-products))))
                                   (assoc-in [machine :process] product))
                               (-> future-events
                                   (conj {::sim-de-event/type :MT
                                          ::sim-de-event/date new-date
                                          ::product product
                                          ::machine machine})
                                   (sim-de-event/postpone-events
                                    (fn [{::sim-de-event/keys [type machine]}]
                                      (and (= machine machine) (= type :MP)))
                                    new-date)))))

(defn machine-terminate
  [{:keys [::sim-de-event/date ::product ::machine]} state future-events]
  (let [transportation-end-time (+ date 2)]
    (sim-de-event-return/build
     (assoc-in state [machine :process] nil)
     (if-let [next-m (rand-nth (get routing machine))]
       (cons {::sim-de-event/type :MA
              ::sim-de-event/date transportation-end-time
              ::product product
              ::machine next-m}
             future-events)
       (cons {::sim-de-event/type :PT
              ::sim-de-event/date transportation-end-time
              ::product product}
             future-events)))))

(defn part-terminate
  [_ state future-events]
  (sim-de-event-return/build state future-events))

(defn state-rendering
  [state]
  (apply println
         (for [mk (keys process-time)]
           (str (or (str/join " " (map name (get-in state [mk :input]))) "_")
                " -> "
                (name mk)
                "("
                (or (some-> (get-in state [mk :process])
                            name)
                    "_")
                ")"))))

(defn registries
  []
  (-> (simulation-core/registries)
      (update :event
              merge
              {:IN evt-init
               :MA machine-arrive
               :MP machine-process
               :MT machine-terminate
               :PT part-terminate})))

(def model-data
  {:initial-event-type :IN
   :ordering [[:field ::sim-de-event/date]
              [:type [:IN :MA :MP :MT :PT]]
              [:field ::machine]
              [:field ::product]]
   :stopping-criterias [[:iteration-nth {:n 100}]]})

(comment
  (def model (simulation-core/build-model model-data (registries)))
  (def full-run
    (simulation-core/scheduler model [[:state-rendering state-rendering]] []))
  (def s1
    (simulation-core/scheduler model
                               []
                               [[:iteration-nth {:n 30}]]))
  (def s2
    (->> s1
         simulation-core/extract-snapshot
         (simulation-core/scheduler
          model
          [[:iteration-nth {:n 30
                            :model-end? true}]]
          [:response-validation :supp-middlewares-insert :request-validation])))
  ;
)
