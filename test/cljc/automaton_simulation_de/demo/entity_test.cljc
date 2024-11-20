(ns automaton-simulation-de.demo.entity-test
  (:require
   #?(:clj [clojure.test :refer [deftest is]]
      :cljs [cljs.test :refer [deftest is] :include-macros true])
   [automaton-optimization.randomness         :as opt-randomness]
   [automaton-simulation-de.demo.entity       :as sut]
   [automaton-simulation-de.simulation-engine :as sim-engine]))

(deftest validation-test
  (is (nil? (sim-engine/validate-model (sut/model))))
  (is (nil? (sim-engine/validate-registry (sut/registries (opt-randomness/xoroshiro128
                                                           [4965052502050351187
                                                            8171162042901641346])))))
  (is (nil? (sim-engine/validate-middleware-data [] {})))
  (is (nil? (sim-engine/validate-stopping-criteria-data [[::sim-engine/iteration-nth
                                                          #::sim-engine{:n 30}]]
                                                        {})))
  "entity demo pass validation")

(deftest run-test
  (is (nil? (sim-engine/validate-response (sim-engine/scheduler (sut/model)))))
  (is (= 12
         (-> (sut/model)
             sim-engine/scheduler
             sim-engine/extract-snapshot
             ::sim-engine/date))))
