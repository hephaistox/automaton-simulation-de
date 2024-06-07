(ns automaton-simulation-de.impl.built-in-sd.response-validation-test
  (:require
   [automaton-core.adapters.schema                               :as
                                                                 core-schema]
   #?(:clj [clojure.test :refer [deftest is testing]]
      :cljs [cljs.test :refer [deftest is testing] :include-macros true])
   [automaton-simulation-de.impl.built-in-sd.response-validation :as sut]
   [automaton-simulation-de.impl.stopping.definition
    :as sim-de-sc-definition]))

(deftest stopping-definition-test
  (testing "Is response-schema complies to stopping-definition schema."
    (is (nil? (->> (sut/stopping-definition)
                   (core-schema/validate-data-humanize
                    sim-de-sc-definition/schema))))))
