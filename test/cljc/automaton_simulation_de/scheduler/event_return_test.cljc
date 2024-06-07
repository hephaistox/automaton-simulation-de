(ns automaton-simulation-de.scheduler.event-return-test
  (:require
   #?(:clj [clojure.test :refer [deftest is testing]]
      :cljs [cljs.test :refer [deftest is testing] :include-macros true])
   [automaton-core.adapters.schema                 :as core-schema]
   [automaton-simulation-de.scheduler.event-return :as sut]))

(deftest schema-test
  (testing "event return has a valid schema"
    (is (nil? (core-schema/validate-humanize sut/schema)))))
