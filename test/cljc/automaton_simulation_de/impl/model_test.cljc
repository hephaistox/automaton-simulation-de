(ns automaton-simulation-de.impl.model-test
  (:require
   #?(:clj [clojure.test :refer [deftest is testing]]
      :cljs [cljs.test :refer [deftest is testing] :include-macros true])
   [automaton-core.adapters.schema        :as core-schema]
   [automaton-simulation-de.impl.model    :as sut]
   [automaton-simulation-de.impl.registry :as sim-de-registry]))

(deftest schema-test (is (nil? (core-schema/validate-humanize sut/schema))))

(deftest build-test
  (testing "Minimal model with registry only is valid."
    (is (nil? (sut/validate (sut/build {:initial-event-type :IN}
                                       (sim-de-registry/build))))))
  (testing "Default registry is valid."
    (is (nil? (-> (sut/build {:initial-event-type :IN} (sim-de-registry/build))
                  sut/validate))))
  (testing "Invalid registry is returning nil."
    (is (-> (sut/build {} [])
            ;(sut/build {:initial-event-type :IN} (sim-de-registry/build))
            sut/validate
            :error
            some?))))
