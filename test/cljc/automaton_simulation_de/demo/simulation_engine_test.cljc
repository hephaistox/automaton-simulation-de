(ns automaton-simulation-de.demo.simulation-engine-test
  (:require
   #?(:clj [clojure.test :refer [deftest is]]
      :cljs [cljs.test :refer [deftest is] :include-macros true])
   [automaton-simulation-de.demo.simulation-engine :as sut]
   [automaton-simulation-de.simulation-engine      :as sim-engine]))

(deftest validation-test
  (is (nil? (sim-engine/validate-model (sut/model))))
  (is (nil? (sim-engine/validate-registry (sut/registries))))
  (is (nil? (sim-engine/validate-middleware-data [[:state-rendering sut/state-rendering]] {})))
  (is (nil? (sim-engine/validate-stopping-criteria-data [[::sim-engine/iteration-nth
                                                          #::sim-engine{:n 30}]]
                                                        {})))
  "simulation engine demo pass validation")

(deftest run-test
  (is (nil? (sim-engine/validate-response (sim-engine/scheduler (sut/model)))))
  (is (= 13
         (-> (sut/model)
             sim-engine/scheduler
             sim-engine/extract-snapshot
             ::sim-engine/date))))
