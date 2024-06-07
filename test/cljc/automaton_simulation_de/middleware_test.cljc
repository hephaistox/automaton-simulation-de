(ns automaton-simulation-de.middleware-test
  (:require
   [automaton-core.adapters.schema     :as core-schema]
   #?(:clj [clojure.test :refer [deftest is testing]]
      :cljs [cljs.test :refer [deftest is testing] :include-macros true])
   [automaton-simulation-de.middleware :as sut]))

(deftest id-schema-test
  (is (nil? (core-schema/validate-humanize sut/id-schema))))

(deftest schema-test
  (testing "Valid schema?"
    (is (nil? (core-schema/validate-humanize sut/schema)))))
