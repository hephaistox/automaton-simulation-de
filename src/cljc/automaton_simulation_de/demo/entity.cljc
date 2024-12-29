#_{:heph-ignore {:forbidden-words ["tap>"]}}
(ns automaton-simulation-de.demo.entity
  (:require
   [automaton-optimization.distribution                    :as opt-distribution]
   [automaton-optimization.maths                           :as opt-maths]
   [automaton-simulation-de.demo.data                      :as sim-demo-data]
   [automaton-simulation-de.entity                         :as sim-entity]
   [automaton-simulation-de.rc                             :as sim-rc]
   [automaton-simulation-de.simulation-engine              :as sim-engine]
   [automaton-simulation-de.simulation-engine.event-return :as sim-de-event-return]))

(defn events
  [prng]
  (let [uniform-distribution (opt-distribution/distribution {:prng prng
                                                             :dst-name :uniform-int})]
    {:CE (fn [{::sim-engine/keys [date]
               ::keys [entity-current entity-max]
               :or {entity-current 1}}
              state
              future-events]
           (let [product-id (keyword (str "p-" entity-current))
                 colors [:blue :purple]
                 random-numb (opt-distribution/draw uniform-distribution)
                 color-idx (opt-maths/mod random-numb (count colors))
                 entity-color (nth colors color-idx)
                 route (case entity-color
                         :blue [{:resource-id :m4
                                 :transportation-time 2
                                 :processing-time (:m4 sim-demo-data/process-time)}
                                {:resource-id :m2
                                 :transportation-time 2
                                 :processing-time (:m2 sim-demo-data/process-time)}
                                {:resource-id :m1
                                 :transportation-time 0
                                 :processing-time (:m1 sim-demo-data/process-time)}]
                         :purple [{:resource-id :m4
                                   :transportation-time 2
                                   :processing-time (:m4 sim-demo-data/process-time)}
                                  {:resource-id :m3
                                   :transportation-time 2
                                   :processing-time (:m3 sim-demo-data/process-time)}
                                  {:resource-id :m1
                                   :transportation-time 0
                                   :processing-time (:m1 sim-demo-data/process-time)}]
                         {})
                 entity-state (sim-entity/create state
                                                 date
                                                 product-id
                                                 {:color entity-color
                                                  :next-machines (pop route)})]
             (cond-> #::sim-engine{:state entity-state
                                   :future-events future-events}
               (< entity-current entity-max) (sim-de-event-return/add-event
                                              {::sim-engine/type :CE
                                               ::sim-engine/date date
                                               ::sim-demo-data/operation (-> route
                                                                             peek)
                                               ::entity-max entity-max
                                               ::entity-current (inc entity-current)})
               true (sim-de-event-return/add-event {::sim-engine/type :MA
                                                    ::sim-engine/date (+ date
                                                                         (-> route
                                                                             peek
                                                                             (:transportation-time
                                                                              0)))
                                                    ::sim-demo-data/product product-id
                                                    ::sim-demo-data/operation (-> route
                                                                                  peek)}))))
     :MA (fn [{::sim-engine/keys [date]
               ::sim-demo-data/keys [product operation]}
              state
              future-events]
           (let [entity-state (sim-entity/update state
                                                 date
                                                 product
                                                 update
                                                 :operations
                                                 conj
                                                 (-> operation
                                                     (assoc :arrival date)))]
             (-> #::sim-engine{:state entity-state
                               :future-events future-events}
                 (sim-rc/seize (:resource-id operation)
                               1
                               date
                               {::sim-engine/type :MP
                                ::sim-demo-data/product product
                                ::sim-demo-data/operation operation}))))
     :MP (fn [{::sim-engine/keys [date]
               ::sim-demo-data/keys [product operation]
               ::sim-rc/keys [resource]}
              state
              future-events]
           (-> #::sim-engine{:state state
                             :future-events future-events}
               (sim-de-event-return/add-event {::sim-engine/type :MT
                                               ::sim-engine/date (+ date
                                                                    (:processing-time operation 0))
                                               ::sim-rc/resource resource
                                               ::sim-demo-data/product product
                                               ::sim-demo-data/operation operation})))
     :MT (fn [{::sim-engine/keys [date]
               ::sim-demo-data/keys [product operation]
               :as current-event}
              state
              future-events]
           (let [next-machines (:next-machines (sim-entity/state state product))
                 next-operation (peek next-machines)
                 r (when (not-empty next-machines) (pop next-machines))
                 transportation-end-time (+ date (:transportation-time next-operation 0))
                 entity-state (-> state
                                  (sim-entity/update date product assoc :next-machines r)
                                  (sim-entity/update date
                                                     product
                                                     update
                                                     :operations
                                                     (fn [ops]
                                                       (conj (pop ops)
                                                             (-> ops
                                                                 peek
                                                                 (assoc :departure date))))))]
             (-> #::sim-engine{:state entity-state
                               :future-events future-events}
                 (sim-rc/dispose (:resource-id operation) current-event)
                 (sim-de-event-return/if-return
                  (empty? next-machines)
                  #(sim-de-event-return/add-event %
                                                  {::sim-engine/type :PT
                                                   ::sim-demo-data/product product}
                                                  transportation-end-time)
                  #(sim-de-event-return/add-event %
                                                  {::sim-engine/type :MA
                                                   ::sim-demo-data/product product
                                                   ::sim-demo-data/operation next-operation}
                                                  transportation-end-time)))))
     :PT (fn [{::sim-engine/keys [date]
               ::sim-demo-data/keys [product]
               :as _current-event}
              state
              future-events]
           (let [entity-state (-> state
                                  (sim-entity/dispose date product))]
             #::sim-engine{:state entity-state
                           :future-events future-events}))}))

(defn registries
  [prng]
  (-> (sim-engine/registries)
      (update ::sim-engine/event merge (events prng))))

(def model-data
  (-> {::sim-engine/stopping-criterias []}
      (assoc ::sim-rc/rc
             {:m1 {}
              :m2 {}
              :m3 {}
              :m4 {}})
      (assoc ::seed #uuid "e85427c1-ed25-4ed4-9b11-52238d268265")
      (assoc ::sim-engine/ordering
             [[::sim-engine/field ::sim-engine/date]
              [::sim-engine/type [:CE :MA :MP :MT :PT]]
              [::sim-engine/field ::sim-demo-data/product]])
      (assoc ::sim-engine/future-events
             [{::sim-engine/type :CE
               ::sim-engine/date 0
               ::entity-max 3}])))

(defn model
  ([model-data registries]
   (-> model-data
       (update ::sim-engine/stopping-criterias
               conj
               [::sim-engine/iteration-nth #::sim-engine{:n 100}])
       (sim-engine/build-model registries)
       (sim-rc/wrap-model (sim-rc/unblocking-policy-registry) (sim-rc/preemption-policy-registry))
       sim-entity/wrap-model))
  ([] (model model-data (registries (sim-demo-data/prng (::seed model-data))))))
