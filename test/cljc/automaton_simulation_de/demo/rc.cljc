#_{:heph-ignore {:forbidden-words ["tap>"]}}
(ns automaton-simulation-de.demo.rc
  "Testing the scheduler with no other lib."
  (:require
   [automaton-simulation-de.event-library.common           :as sim-de-common]
   [automaton-simulation-de.rc                             :as sim-rc]
   [automaton-simulation-de.simulation-engine              :as sim-engine]
   [automaton-simulation-de.simulation-engine.event-return :as sim-de-event-return]))

(defn state-rendering
  [state]
  (let [resources (vals (get-in state [::sim-rc/resource]))]
    (apply concat
           (for [{::sim-rc/keys [name currently-consuming queue]} resources]
             (concat
              (mapv (fn [consumption]
                      (clojure.core/name (get-in consumption [::sim-rc/seizing-event ::product])))
                    queue)
              ["->" (clojure.core/name name) "("]
              (map (fn [consumption]
                     (clojure.core/name (get-in consumption [::sim-rc/seizing-event ::product])))
                   (vals currently-consuming))
              [")"])))))

(defn registries
  []
  (->
    (sim-engine/registries)
    (update
     ::sim-engine/event
     merge
     {:IN (sim-de-common/init-events [{::sim-engine/type :MA
                                       ::product :p1
                                       ::machine :m1}
                                      {::sim-engine/type :MA
                                       ::product :p2
                                       ::machine :m1}
                                      {::sim-engine/type :MA
                                       ::product :p3
                                       ::machine :m1}]
                                     0)
      :MA (fn [{::sim-engine/keys [date]
                ::keys [product machine]}
               state
               future-events]
            (-> #:automaton-simulation-de.simulation-engine{:state state
                                                            :future-events future-events}
                (sim-rc/seize machine
                              1
                              date
                              {::sim-engine/type :MP
                               ::product product
                               ::machine machine})))
      :MP (fn [{::sim-engine/keys [date]
                ::keys [product machine]
                ::sim-rc/keys [resource]}
               state
               future-events]
            (-> #:automaton-simulation-de.simulation-engine{:state state
                                                            :future-events future-events}
                (sim-de-event-return/add-event {::sim-engine/type :MT
                                                ::sim-engine/date (+ date
                                                                     (get {:m1 1
                                                                           :m2 3
                                                                           :m3 3
                                                                           :m4 1}
                                                                          machine))
                                                ::sim-rc/resource resource
                                                ::product product
                                                ::machine machine})))
      :MT (fn [{::sim-engine/keys [date]
                ::keys [product machine]
                :as current-event}
               state
               future-events]
            (let [transportation-end-time (+ date 2)
                  next-machines (get {:m1 [:m2 :m3]
                                      :m2 [:m4]
                                      :m3 [:m4]}
                                     machine)]
              (-> #:automaton-simulation-de.simulation-engine{:state state
                                                              :future-events future-events}
                  (sim-rc/dispose machine current-event)
                  (sim-de-event-return/if-return
                   (empty? next-machines)
                   #(sim-de-event-return/add-event %
                                                   {::sim-engine/type :PT
                                                    ::product product}
                                                   transportation-end-time)
                   #(sim-de-event-return/nth %
                                             (map (fn [next-machine]
                                                    {::sim-engine/type :MA
                                                     ::product product
                                                     ::machine next-machine})
                                                  next-machines)
                                             transportation-end-time)))))
      :PT sim-de-common/sink})))

(def model-data
  {::sim-engine/initial-event-type :IN
   ::sim-engine/initial-bucket 0
   ::sim-engine/ordering [[::sim-engine/field ::sim-engine/date]
                          [::sim-engine/type [:IN :MA :MP :MT :PT]]
                          [::sim-engine/field ::machine]
                          [::sim-engine/field ::product]]
   ::sim-engine/stopping-criterias [[::sim-engine/iteration-nth {::sim-engine/n 100}]]
   :rc {:m1 {}
        :m2 {}
        :m3 {}
        :m4 {}}})

(def model
  (-> model-data
      (sim-engine/build-model (registries))
      (sim-rc/wrap-model model-data
                         (sim-rc/unblocking-policy-registry)
                         (sim-rc/preemption-policy-registry))))

(comment
  (def full-run
    (sim-engine/scheduler model
                          [[::sim-engine/state-printing state-rendering]]
                          [::sim-engine/response-validation ::sim-engine/request-validation]))
  (def s1
    (sim-engine/scheduler model
                          [::sim-engine/response-validation ::sim-engine/request-validation]
                          [[::sim-engine/iteration-nth {::sim-engine/n 1}]]))
  (def s2
    (->> s1
         sim-engine/extract-snapshot
         (sim-engine/scheduler model
                               [::sim-engine/response-validation ::sim-engine/request-validation]
                               [[::sim-engine/iteration-nth {::sim-engine/n 10}]])))
  (-> s2
      sim-engine/validate-response)
  (def s3
    (->> s1
         sim-engine/extract-snapshot
         (sim-engine/scheduler model [::sim-engine/tap-response ::sim-engine/tap-request] [])))
  (-> s3
      sim-engine/validate-response)
  ;
)
