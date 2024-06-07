(ns automaton-simulation-de.impl.built-in-sd.execution-not-found-test
  (:require
   #?(:clj [clojure.test :refer [deftest is]]
      :cljs [cljs.test :refer [deftest is] :include-macros true])
   [automaton-core.adapters.schema                               :as
                                                                 core-schema]
   [automaton-simulation-de.impl.built-in-sd.execution-not-found :as sut]
   [automaton-simulation-de.impl.stopping.definition
    :as sim-de-sc-definition]
   [automaton-simulation-de.response
    :as sim-de-response]
   [automaton-simulation-de.scheduler.snapshot
    :as sim-de-snapshot]))

(deftest stopping-definition-test
  (is (nil? (->> sut/stopping-definition
                 (core-schema/validate-data-humanize
                  sim-de-sc-definition/schema)))))

(deftest evaluates-test
  (is (nil? (->> (sut/evaluates (sim-de-response/build
                                 []
                                 (sim-de-snapshot/build 1 1 1 {} [] []))
                                nil)
                 (core-schema/validate-data-humanize sim-de-response/schema)))))
