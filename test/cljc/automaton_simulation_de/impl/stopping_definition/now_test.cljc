(ns automaton-simulation-de.impl.stopping-definition.now-test
  (:require
   #?(:clj [clojure.test :refer [deftest is]]
      :cljs [cljs.test :refer [deftest is] :include-macros true])
   [automaton-core.adapters.schema                       :as core-schema]
   [automaton-simulation-de.impl.stopping-definition.now :as sut]
   [automaton-simulation-de.impl.stopping.definition     :as
                                                         sim-de-sc-definition]))

(deftest stop-now-test
  (is (= {:stop? true
          :context nil}
         (sut/stop-now nil nil))))

(deftest stopping-definition-test
  (is (nil? (->> (sut/stopping-definition)
                 (core-schema/validate-data-humanize
                  sim-de-sc-definition/schema)))))

