(ns automaton-simulation-de.demo.data
  (:require
   [automaton-optimization.randomness         :as opt-randomness]
   [automaton-simulation-de.simulation-engine :as-alias sim-engine]))

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

(def model-data
  (let [date 0]
    #::sim-engine{:ordering [[::sim-engine/field ::sim-engine/date]
                             [::sim-engine/type [:MA :MP :MT :PT]]
                             [::sim-engine/field ::machine]
                             [::sim-engine/field ::product]]
                  :future-events [{::sim-engine/type :MA
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
                                   ::machine :m1}]
                  :stopping-criterias []}))

(defn prng [seed] (opt-randomness/xoroshiro128 seed))
