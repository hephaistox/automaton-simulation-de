(ns automaton-simulation-de.event-registry-test
  (:require
   #?(:clj [clojure.test :refer [deftest is testing]]
      :cljs [cljs.test :refer [deftest is testing] :include-macros true])
   [automaton-core.adapters.schema         :as core-schema]
   [automaton-simulation-de.event-registry :as sut]))

(deftest schema-test
  (testing "event registry has a valid schema"
    (is (nil? (core-schema/validate-humanize sut/schema)))))
