(ns automaton-simulation-de.demo.toy-example
  "Simulation domain, that can be tested from console
   Used to help during development of simulation de concepts"
  (:require
   [clojure.string :as str]
   [automaton-simulation-de.rendering :as sim-de-rendering]
   [automaton-simulation-de.scheduler :as sim-de-scheduler]
   [automaton-simulation-de.event :as sim-de-event]
   [automaton-simulation-de.scheduler.middleware
    :as
    sim-de-scheduler-middleware]
   [automaton-core.utils.map :as utils-map]))

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


;; Event registry

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

(def ^:private event-registry
  {:event-registry-kvs {:IN evt-init
                        :MA machine-arrive
                        :MP machine-process
                        :MT machine-terminate
                        :PT part-terminate}
   :event-ordering {:same-date-ordering same-date-ordering}})

;; Middleware
(defn state-rendering
  [state]
  (let [state (utils-map/remove-nil-submap-vals state)]
    (apply str
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
               vec))))

(defn customer-stopping-criteria
  [{:keys [id]
    :as scheduler-iteration}]
  (if (> id 500)
    (sim-de-scheduler-middleware/stop-iteration scheduler-iteration)
    scheduler-iteration))


(defn run
  [stopping-criteria-fn]
  (let [scheduler-first-iteration {:id 0
                                   :state {}
                                   :past-events []
                                   :future-events [{:type :IN
                                                    :date 0}]}
        {:keys [id state past-events future-events]}
        (sim-de-scheduler/scheduler
         event-registry
         scheduler-first-iteration
         [(partial sim-de-rendering/scheduler-iteration state-rendering)
          stopping-criteria-fn])]
    (println)
    (println ">>> End of simulation")
    (println "iteration number is:" (pr-str id))
    (println "state is " (pr-str state))
    (println "past events: " (pr-str past-events))
    (println "future events: " (pr-str future-events))))

(comment
  (run customer-stopping-criteria)
  ;
)
