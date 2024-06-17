(ns
  automaton-simulation-de.simulation-engine.impl.built-in-sd.request-validation-test
  (:require
   #?(:clj [clojure.test :refer [deftest is]]
      :cljs [cljs.test :refer [deftest is] :include-macros true])
   [automaton-core.adapters.schema
    :as core-schema]
   [automaton-simulation-de.simulation-engine.impl.built-in-sd.request-validation
    :as sut]
   [automaton-simulation-de.simulation-engine.impl.stopping.definition
    :as sim-de-sc-definition]))

(deftest stopping-definition-test
  (is (= nil
         (->> (sut/stopping-definition)
              (core-schema/validate-data-humanize sim-de-sc-definition/schema)))
      "Valid schema."))
