(ns automaton-simulation-de.simulation-engine.orderings-test
  (:require
   #?(:clj [clojure.test :refer [deftest is]]
      :cljs [cljs.test :refer [deftest is] :include-macros true])
   [automaton-core.adapters.schema                      :as core-schema]
   [automaton-simulation-de.simulation-engine.orderings :as sut]))

(deftest schema-test (is (= nil (core-schema/validate-humanize sut/schema))))
