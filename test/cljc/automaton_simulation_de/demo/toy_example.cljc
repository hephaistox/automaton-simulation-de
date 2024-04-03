#_{:heph-ignore {:forbidden-words ["tap>"]}}
(ns automaton-simulation-de.demo.toy-example
  "Simulation domain, that can be tested from console
   Used to help during development of simulation de concepts"
  (:require
   [automaton-core.utils.map :as utils-map]
   [automaton-simulation-de.ordering :as sim-de-event-ordering]
   [automaton-simulation-de.scheduler :as sim-de-scheduler]
   [automaton-simulation-de.scheduler.event :as sim-de-event]
   [automaton-simulation-de.scheduler.event-return :as sim-de-event-return]
   [automaton-simulation-de.scheduler.snapshot :as sim-de-snapshot]
   [automaton-simulation-de.middleware.schema-validation :as sim-de-schema-validation]
   [clojure.string :as str]))

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
  (sim-de-event-return/build (-> state
                                 (update-in [machine :input]
                                            (fn [list-products]
                                              (vec (conj list-products product)))))

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
                                                (vec (remove #{product} list-products))))
                                   (assoc-in [machine :process] product))
                               (-> future-events
                                   (conj {::sim-de-event/type :MT
                                          ::sim-de-event/date new-date
                                          ::product product
                                          ::machine machine})
                                   (sim-de-event/postpone-events
                                    (fn [{:keys [type machine]}]
                                      (and (= machine machine) (= type :MP)))
                                    new-date)))))

(defn machine-terminate
  [{:keys [::sim-de-event/date ::product ::machine]} state future-events]
  (let [transportation-end-time (+ date 2)]
    (sim-de-event-return/build (assoc-in state [machine :process] nil)
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

;; Middleware
(defn state-rendering
  [state]
  (let [state (utils-map/remove-nil-submap-vals state)]
    (println (apply str
                    (-> (for [mk (keys process-time)]
                          (str (or (str/join " " (map name (get-in state [mk :input])))
                                   "_")
                               " -> "
                               (name mk)
                               "("
                               (or (some-> (get-in state [mk :process])
                                           name)
                                   "_")
                               ")"))
                        vec)))))

(defn toy-example-schedule
  []
  (sim-de-scheduler/scheduler {:IN evt-init
                               :MA machine-arrive
                               :MP machine-process
                               :MT machine-terminate
                               :PT part-terminate}
                              [sim-de-schema-validation/request-validation
                               (partial sim-de-schema-validation/state-rendering state-rendering)
                               sim-de-schema-validation/response-validation]
                              [(sim-de-event-ordering/compare-field ::sim-de-event/date)
                               (sim-de-event-ordering/compare-types [:IN :MA :MP :MT :PT])
                               (sim-de-event-ordering/compare-field ::machine)
                               (sim-de-event-ordering/compare-field ::product)]
                              (sim-de-snapshot/build 1 1 0
                                                     {}
                                                     []
                                                     (sim-de-event/make-events :IN 0))
                              100))
(comment
  (tap> [:toy-example-end (toy-example-schedule)])
  ;
  )
