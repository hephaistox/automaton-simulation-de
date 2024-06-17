(ns automaton-simulation-de.simulation-engine.event-execution-test
  (:require
   #?(:clj [clojure.test :refer [deftest is testing]]
      :cljs [cljs.test :refer [deftest is testing] :include-macros true])
   [automaton-core.adapters.schema                            :as core-schema]
   [automaton-simulation-de.simulation-engine.event-execution :as sut]))

(deftest schema-test
  (testing "event execution has a valid schema"
    (is (= nil (core-schema/validate-humanize sut/schema)))))
