(ns automaton-simulation-de.simulation-engine.impl.built-in-sd.registry-test
  (:require
   #?(:clj [clojure.test :refer [deftest is]]
      :cljs [cljs.test :refer [deftest is] :include-macros true])
   [automaton-core.adapters.schema
    :as core-schema]
   [automaton-simulation-de.simulation-engine.impl.built-in-sd.registry :as
                                                                        sut]))

(deftest build-test
  (is (= nil (core-schema/validate-data-humanize sut/schema (sut/build)))
      "Is registry matching the schema.")
  (is
   (= (count sut/stopping-definitions) (count (sut/build)))
   "Check that no definition is missing (can disappear if id is not well managed)."))
