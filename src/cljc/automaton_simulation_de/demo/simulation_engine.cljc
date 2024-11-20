#_{:heph-ignore {:forbidden-words ["tap>"]}}
(ns automaton-simulation-de.demo.simulation-engine
  "Simulation domain, that can be tested from console
   Used to help during development of simulation de concepts"
  (:require
   [automaton-simulation-de.demo.data                      :as sim-demo-data]
   [automaton-simulation-de.event-library.common           :as sim-de-common]
   [automaton-simulation-de.simulation-engine              :as sim-engine]
   [automaton-simulation-de.simulation-engine.event        :as sim-de-event]
   [automaton-simulation-de.simulation-engine.event-return :as sim-de-event-return]
   [clojure.string                                         :as str]))

;; print
(defn state-rendering
  [state]
  (apply println
         (for [mk (keys sim-demo-data/process-time)]
           (str (or (str/join " " (map name (get-in state [mk :input]))) "_")
                " -> "
                (name mk)
                "("
                (or (some-> (get-in state [mk :process])
                            name)
                    "_")
                ")"))))

;; Events
(def events
  {:MA (fn [{::sim-engine/keys [date]
             ::sim-demo-data/keys [product machine]}
            state
            future-events]
         (-> #::sim-engine{:state (-> state
                                      (update-in [machine :input]
                                                 (fn [list-products]
                                                   (vec (conj list-products product)))))
                           :future-events future-events}
             (sim-de-event-return/add-event {::sim-engine/type :MP
                                             ::sim-engine/date date
                                             ::sim-demo-data/product product
                                             ::sim-demo-data/machine machine})))
   :MP (fn [{::sim-engine/keys [date]
             ::sim-demo-data/keys [product machine]}
            state
            future-events]
         (let [new-date (+ date (sim-demo-data/process-time machine))]
           (-> #::sim-engine{:state (-> state
                                        (update-in [machine :input]
                                                   (fn [list-products]
                                                     (vec (remove #{product} list-products))))
                                        (assoc-in [machine :process] product))
                             :future-events future-events}
               (sim-de-event-return/add-event {::sim-engine/type :MT
                                               ::sim-engine/date new-date
                                               ::sim-demo-data/product product
                                               ::sim-demo-data/machine machine})
               (update ::sim-engine/future-events
                       #(sim-de-event/postpone-events
                         %
                         (fn [{::sim-engine/keys [type]
                               :as evt}]
                           (and (= (::sim-demo-data/machine evt) machine) (= type :MP)))
                         new-date)))))
   :MT (fn [{::sim-engine/keys [date]
             ::sim-demo-data/keys [product machine]}
            state
            future-events]
         (let [transportation-end-time (+ date 2)
               next-machines (get sim-demo-data/routing machine)]
           (-> #::sim-engine{:state (assoc-in state [machine :process] nil)
                             :future-events future-events}
               (sim-de-event-return/if-return
                (empty? next-machines)
                #(sim-de-event-return/add-event %
                                                {::sim-engine/type :PT
                                                 ::sim-demo-data/product product}
                                                transportation-end-time)
                #(sim-de-event-return/nth %
                                          (map (fn [next-machine]
                                                 {::sim-engine/type :MA
                                                  ::sim-demo-data/product product
                                                  ::sim-demo-data/machine next-machine})
                                               next-machines)
                                          transportation-end-time)))))
   :PT sim-de-common/sink})

(defn registries
  []
  (-> (sim-engine/registries)
      (update ::sim-engine/event merge events)))

(defn model
  ([model-data registries]
   (-> model-data
       (update ::sim-engine/stopping-criterias
               conj
               [::sim-engine/iteration-nth #::sim-engine{:n 100}])
       (sim-engine/build-model registries)))
  ([] (model sim-demo-data/model-data (registries))))
