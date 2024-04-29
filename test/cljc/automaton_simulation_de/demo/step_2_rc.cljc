#_{:heph-ignore {:forbidden-words ["tap>"]}}
(ns automaton-simulation-de.demo.step-2-rc
  "Testing the scheduler with no other lib.
  Everything is manually assembled here."
  (:require
   [automaton-simulation-de.core                   :as simulation-core]
   [automaton-simulation-de.event-library.common   :as sim-de-common]
   [automaton-simulation-de.impl.scheduler         :as sim-de-scheduler]
   [automaton-simulation-de.middleware             :as sim-de-middleware]
   [automaton-simulation-de.ordering               :as sim-de-ordering]
   [automaton-simulation-de.rc                     :as sim-de-rc]
   [automaton-simulation-de.scheduler.event        :as sim-de-event]
   [automaton-simulation-de.scheduler.event-return :as sim-de-event-return]))

;; Middleware
(defn state-rendering
  [state]
  (let [resources (vals (get-in state [::sim-de-rc/resource]))]
    (apply concat
           (for [{::sim-de-rc/keys [name currently-consuming queue]} resources]
             (concat (mapv (fn [consumption]
                             (clojure.core/name
                              (get-in consumption
                                      [::sim-de-rc/seizing-event ::product])))
                           queue)
                     ["->" (clojure.core/name name) "("]
                     (map (fn [consumption]
                            (clojure.core/name
                             (get-in consumption
                                     [::sim-de-rc/seizing-event ::product])))
                          (vals currently-consuming))
                     [")"])))))

(defn toy-example-schedule
  []
  (let [model
        (simulation-core/build-model
         {:IN (sim-de-common/init-events [{::sim-de-event/type :MA
                                           ::product :p1
                                           ::machine :m1}
                                          {::sim-de-event/type :MA
                                           ::product :p2
                                           ::machine :m1}
                                          {::sim-de-event/type :MA
                                           ::product :p3
                                           ::machine :m1}]
                                         0)
          :MA (fn [{:keys [::sim-de-event/date ::product ::machine]}
                   state
                   future-events]
                (-> (sim-de-event-return/build state future-events)
                    (sim-de-rc/seize machine
                                     1
                                     date
                                     {::sim-de-event/type :MP
                                      ::product product
                                      ::machine machine})))
          :MP
          (fn [{:keys
                [::sim-de-event/date ::product ::machine ::sim-de-rc/resource]}
               state
               future-events]
            (-> (sim-de-event-return/build state future-events)
                (sim-de-event-return/add-event {::sim-de-event/type :MT
                                                ::sim-de-event/date
                                                (+ date
                                                   (get {:m1 1
                                                         :m2 3
                                                         :m3 3
                                                         :m4 1}
                                                        machine))
                                                ::sim-de-rc/resource resource
                                                ::product product
                                                ::machine machine})))
          :MT (fn [{:keys [::sim-de-event/date ::product ::machine]
                    :as current-event}
                   state
                   future-events]
                (let [transportation-end-time (+ date 2)
                      next-machines (get {:m1 [:m2 :m3]
                                          :m2 [:m4]
                                          :m3 [:m4]}
                                         machine)]
                  (-> (sim-de-event-return/build state future-events)
                      (sim-de-rc/dispose machine current-event)
                      (sim-de-event-return/if-return
                       (empty? next-machines)
                       #(sim-de-event-return/add-event %
                                                       {::sim-de-event/type :PT
                                                        ::product product}
                                                       transportation-end-time)
                       #(sim-de-event-return/nth %
                                                 (map (fn [next-machine]
                                                        {::sim-de-event/type :MA
                                                         ::product product
                                                         ::machine
                                                         next-machine})
                                                      next-machines)
                                                 transportation-end-time)))))
          :PT sim-de-common/sink}
         [; sim-de-middleware/wrap-response
          sim-de-middleware/response-validation-mdw
          (partial simulation-core/state-printing state-rendering)
          sim-de-middleware/request-validation-mdw
          sim-de-middleware/wrap-request]
         [(sim-de-ordering/compare-field ::sim-de-event/date)
          (sim-de-ordering/compare-types [:IN :MA :MP :MT :PT])
          (sim-de-ordering/compare-field ::machine)
          (sim-de-ordering/compare-field ::product)]
         (simulation-core/initial :IN 0)
         100)]
    (-> model
        (sim-de-rc/wrap-model {:m1 {}
                               :m2 {}
                               :m3 {}
                               :m4 {}})
        sim-de-scheduler/scheduler)))

(comment
  (tap> [:toy-example-end (toy-example-schedule)])
  ;
)
