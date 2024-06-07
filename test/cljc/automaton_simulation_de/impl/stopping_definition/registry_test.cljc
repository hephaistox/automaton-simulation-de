(ns automaton-simulation-de.impl.stopping-definition.registry-test
  (:require
   #?(:clj [clojure.test :refer [deftest is testing]]
      :cljs [cljs.test :refer [deftest is testing] :include-macros true])
   [automaton-core.adapters.schema                            :as core-schema]
   [automaton-simulation-de.impl.stopping-definition.registry :as sut]))

(deftest schema-test (is (nil? (core-schema/validate-humanize sut/schema))))

(deftest build-test
  (testing "Default built-in registry is valid."
    (is (nil? (core-schema/validate-data-humanize sut/schema (sut/build))))))
