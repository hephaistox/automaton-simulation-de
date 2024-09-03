(ns automaton-simulation-de.rc.impl.unblocking-policy.registry-test
  (:require
   #?(:clj [clojure.test :refer [deftest is testing]]
      :cljs [cljs.test :refer [deftest is testing] :include-macros true])
   [automaton-core.adapters.schema                             :as core-schema]
   [automaton-simulation-de.rc.impl.unblocking-policy.registry :as sut]))

(deftest schema-test
  (testing "Validate schema" (is (nil? (core-schema/validate-humanize (sut/schema))))))

(deftest registry-test
  (testing "Validate registry"
    (is (nil? (core-schema/validate-data-humanize (sut/schema) (sut/registry))))))
