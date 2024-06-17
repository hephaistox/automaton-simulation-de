#_{:heph-ignore {:forbidden-words ["tap>"]}}
(ns automaton-simulation-de.demo.simulation-engine
  "Simulation domain, that can be tested from console
   Used to help during development of simulation de concepts"
  (:require
   [automaton-simulation-de.simulation-engine       :as sim-engine]
   [automaton-simulation-de.simulation-engine.event :as sim-de-event]
   [clojure.string                                  :as str]))

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
  [{::sim-engine/keys [date]} _state future-events]
  #:automaton-simulation-de.simulation-engine{:state {}
                                              :future-events
                                              (-> future-events
                                                  (concat
                                                   [{::sim-engine/type :MA
                                                     ::sim-engine/date date
                                                     ::product :p1
                                                     ::machine :m1}
                                                    {::sim-engine/type :MA
                                                     ::sim-engine/date date
                                                     ::product :p2
                                                     ::machine :m1}
                                                    {::sim-engine/type :MA
                                                     ::sim-engine/date date
                                                     ::product :p3
                                                     ::machine :m1}]))})

(defn machine-arrive
  "Product `p` is added on machine `m` input buffer at date `d`
  Creates a new event machine start for the same product `p` starts on machine `m` at date `d`"
  [{::sim-engine/keys [date]
    ::keys [product machine]}
   state
   future-events]
  #:automaton-simulation-de.simulation-engine{:state (-> state
                                                         (update-in
                                                          [machine :input]
                                                          (fn [list-products]
                                                            (vec (conj
                                                                  list-products
                                                                  product)))))
                                              :future-events
                                              (->> future-events
                                                   (cons {::sim-engine/type :MP
                                                          ::sim-engine/date date
                                                          ::product product
                                                          ::machine machine}))})

(defn machine-process
  [{::sim-engine/keys [date]
    ::keys [product machine]}
   state
   future-events]
  (let [new-date (+ date (process-time machine))]
    #:automaton-simulation-de.simulation-engine{:state
                                                (-> state
                                                    (update-in
                                                     [machine :input]
                                                     (fn [list-products]
                                                       (vec (remove
                                                             #{product}
                                                             list-products))))
                                                    (assoc-in [machine :process]
                                                              product))
                                                :future-events
                                                (->
                                                  future-events
                                                  (conj {::sim-engine/type :MT
                                                         ::sim-engine/date
                                                         new-date
                                                         ::product product
                                                         ::machine machine})
                                                  (sim-de-event/postpone-events
                                                   (fn [{::sim-engine/keys
                                                         [type machine]}]
                                                     (and (= machine machine)
                                                          (= type :MP)))
                                                   new-date))}))

(defn machine-terminate
  [{::sim-engine/keys [date]
    ::keys [product machine]}
   state
   future-events]
  (let [transportation-end-time (+ date 2)]
    #:automaton-simulation-de.simulation-engine{:state (assoc-in state
                                                        [machine :process]
                                                        nil)
                                                :future-events
                                                (if-let [next-m (rand-nth
                                                                 (get routing
                                                                      machine))]
                                                  (cons {::sim-engine/type :MA
                                                         ::sim-engine/date
                                                         transportation-end-time
                                                         ::product product
                                                         ::machine next-m}
                                                        future-events)
                                                  (cons {::sim-engine/type :PT
                                                         ::sim-engine/date
                                                         transportation-end-time
                                                         ::product product}
                                                        future-events))}))

(defn part-terminate
  [_ state future-events]
  #:automaton-simulation-de.simulation-engine{:state state
                                              :future-events future-events})

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
  (-> (sim-engine/registries)
      (update ::sim-engine/event
              merge
              {:IN evt-init
               :MA machine-arrive
               :MP machine-process
               :MT machine-terminate
               :PT part-terminate})))

(def model-data
  #:automaton-simulation-de.simulation-engine{:initial-event-type :IN
                                              :ordering
                                              [[::sim-engine/field
                                                ::sim-engine/date]
                                               [::sim-engine/type
                                                [:IN :MA :MP :MT :PT]]
                                               [::sim-engine/field ::machine]
                                               [::sim-engine/field ::product]]
                                              :stopping-criterias
                                              [[::sim-engine/iteration-nth
                                                #:automaton-simulation-de.simulation-engine{:n
                                                                                            100}]]})

(comment
  (def model (sim-engine/build-model model-data (registries)))
  (sim-engine/validate-model model)
  (sim-engine/validate-middleware-data [[:state-rendering state-rendering]] {})
  (sim-engine/validate-stopping-criteria-data
   [[::sim-engine/iteration-nth
     #:automaton-simulation-de.simulation-engine{:n 30}]]
   {})
  (def full-run
    (sim-engine/scheduler model [[:state-rendering state-rendering]] []))
  (def s1
    (sim-engine/scheduler
     model
     [:tap-request :tap-resonse]
     [[:automaton-simulation-de.simulation-engine/iteration-nth
       :automaton-simulation-de.simulation-engine
       {:n 30}]]))
  (def s2
    (->> s1
         sim-engine/extract-snapshot
         (sim-engine/scheduler
          model
          [[:automaton-simulation-de.simulation-engine/iteration-nth
            :automaton-simulation-de.simulation-engine
            {:n 30
             :model-end? true}]]
          [:response-validation :supp-middlewares-insert :request-validation])))
  ;
)
