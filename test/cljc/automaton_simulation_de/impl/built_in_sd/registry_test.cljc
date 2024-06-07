(ns automaton-simulation-de.impl.built-in-sd.registry-test
  (:require
   #?(:clj [clojure.test :refer [deftest is testing]]
      :cljs [cljs.test :refer [deftest is testing] :include-macros true])
   [automaton-core.adapters.schema                    :as core-schema]
   [automaton-simulation-de.impl.built-in-sd.registry :as sut]))

(deftest build-test
  (testing "Is registry well formed."
    (is (nil? (core-schema/validate-data-humanize (sut/schema) (sut/build))))))
