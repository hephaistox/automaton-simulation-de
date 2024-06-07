#_{:heph-ignore {:forbidden-words ["tap>"]}}
(ns automaton-simulation-de.demo.control
  "Demo usefull for control toy example"
  (:require
   [automaton-simulation-de.control                :as sim-de-control]
   [automaton-simulation-de.core                   :as simulation-core]
   [automaton-simulation-de.scheduler.event        :as sim-de-event]
   [automaton-simulation-de.scheduler.event-return :as sim-de-event-return]))

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

(defn infinite-part-terminate
  [{:keys [::product ::machine ::sim-de-event/date]} state future-events]
  (sim-de-event-return/build state
                             (-> future-events
                                 (conj {::sim-de-event/type :IN
                                        ::sim-de-event/date date
                                        ::product product
                                        ::machine machine}))))

(defn registries
  ([] (registries false))
  ([infinite?]
   (-> (simulation-core/registries)
       (update :event
               merge
               {:IN evt-init
                :MA machine-arrive
                :MP machine-process
                :MT machine-terminate
                :PT (if infinite? infinite-part-terminate part-terminate)})
       sim-de-control/wrap-registry)))

(def model-data
  {:initial-event-type :IN
   :initial-bucket 0
   :middlewares []
   :ordering [[:field ::sim-de-event/date]
              [:type [:IN :MA :MP :MT :PT]]
              [:field ::machine]
              [:field ::product]]
   :stopping-criterias []})

(defn model
  []
  (simulation-core/build-model (update model-data
                                       :stopping-criterias
                                       conj
                                       [:iteration-nth {:n 1000}])
                               (registries)))

(defn model-infinite
  []
  (simulation-core/build-model model-data (registries true)))

(defn model-early-end
  []
  (simulation-core/build-model (update model-data
                                       :stopping-criterias
                                       conj
                                       [:iteration-nth {:n 20}])
                               (registries)))
