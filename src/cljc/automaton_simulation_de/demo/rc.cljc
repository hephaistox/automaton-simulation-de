#_{:heph-ignore {:forbidden-words ["tap>"]}}
(ns automaton-simulation-de.demo.rc
  "Testing the scheduler with no other lib."
  (:require
   [automaton-optimization.distribution                    :as opt-distribution]
   [automaton-optimization.maths                           :as opt-maths]
   [automaton-simulation-de.demo.data                      :as sim-demo-data]
   [automaton-simulation-de.event-library.common           :as sim-de-common]
   [automaton-simulation-de.rc                             :as sim-rc]
   [automaton-simulation-de.simulation-engine              :as sim-engine]
   [automaton-simulation-de.simulation-engine.event-return :as sim-de-event-return]))

(defn state-rendering
  [state]
  (let [resources (vals (get-in state [::sim-rc/resource]))]
    (apply concat
           (for [{::sim-rc/keys [name currently-consuming queue]} resources]
             (concat (mapv (fn [consumption]
                             (clojure.core/name (get-in consumption
                                                        [::sim-rc/seizing-event
                                                         ::sim-demo-data/product])))
                           queue)
                     ["->" (clojure.core/name name) "("]
                     (map (fn [consumption]
                            (clojure.core/name
                             (get-in consumption [::sim-rc/seizing-event ::sim-demo-data/product])))
                          (vals currently-consuming))
                     [")"])))))

(defn events
  [prng]
  (let [unif-distribution (opt-distribution/distribution {:prng prng
                                                          :distribution-name :uniform-int
                                                          :params {}})]
    {:MA (fn [{::sim-engine/keys [date]
               ::sim-demo-data/keys [product machine]}
              state
              future-events]
           (-> #::sim-engine{:state state
                             :future-events future-events}
               (sim-rc/seize machine
                             1
                             date
                             {::sim-engine/type :MP
                              ::sim-demo-data/product product
                              ::sim-demo-data/machine machine})))
     :MP (fn [{::sim-engine/keys [date]
               ::sim-demo-data/keys [product machine]
               ::sim-rc/keys [resource]}
              state
              future-events]
           (-> #::sim-engine{:state state
                             :future-events future-events}
               (sim-de-event-return/add-event {::sim-engine/type :MT
                                               ::sim-engine/date
                                               (+ date (get sim-demo-data/process-time machine))
                                               ::sim-rc/resource resource
                                               ::sim-demo-data/product product
                                               ::sim-demo-data/machine machine})))
     :MT (fn [{::sim-engine/keys [date]
               ::sim-demo-data/keys [product machine]
               :as current-event}
              state
              future-events]
           (let [transportation-end-time (+ date 2)
                 next-machines (get sim-demo-data/routing machine)]
             (-> #::sim-engine{:state state
                               :future-events future-events}
                 (sim-rc/dispose machine current-event)
                 (sim-de-event-return/if-return
                  (empty? next-machines)
                  #(sim-de-event-return/add-event %
                                                  {::sim-engine/type :PT
                                                   ::sim-demo-data/product product}
                                                  transportation-end-time)
                  #(let [next-machine-idx (opt-maths/mod (opt-distribution/draw unif-distribution)
                                                         (count next-machines))
                         next-machine (nth next-machines next-machine-idx)]
                     (sim-de-event-return/add-event %
                                                    {::sim-engine/type :MA
                                                     ::sim-demo-data/product product
                                                     ::sim-demo-data/machine next-machine}
                                                    transportation-end-time))))))
     :PT sim-de-common/sink}))

(defn registries
  [prng]
  (-> (sim-engine/registries)
      (update ::sim-engine/event merge (events prng))))

(def model-data
  (-> sim-demo-data/model-data
      (assoc ::seed #uuid "e85427c1-ed25-4ed4-9b11-52238d268265")
      (assoc ::sim-rc/rc
             {:m1 {}
              :m2 {}
              :m3 {}
              :m4 {}})))

(defn model
  ([model-data registries]
   (-> model-data
       (update ::sim-engine/stopping-criterias
               conj
               [::sim-engine/iteration-nth #::sim-engine{:n 100}])
       (sim-engine/build-model registries)
       (sim-rc/wrap-model (sim-rc/unblocking-policy-registry) (sim-rc/preemption-policy-registry))))
  ([] (model model-data (registries (sim-demo-data/prng (::seed model-data))))))
