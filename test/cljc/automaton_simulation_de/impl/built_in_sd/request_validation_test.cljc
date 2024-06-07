(ns automaton-simulation-de.impl.built-in-sd.request-validation-test
  (:require
   #?(:clj [clojure.test :refer [deftest is testing]]
      :cljs [cljs.test :refer [deftest is testing] :include-macros true])
   [automaton-core.adapters.schema                              :as core-schema]
   [automaton-simulation-de.impl.built-in-sd.request-validation :as sut]
   [automaton-simulation-de.impl.stopping.definition
    :as sim-de-sc-definition]))

(deftest stopping-definition-test
  (testing "Valid schema."
    (is (nil? (->> (sut/stopping-definition)
                   (core-schema/validate-data-humanize
                    sim-de-sc-definition/schema))))))
