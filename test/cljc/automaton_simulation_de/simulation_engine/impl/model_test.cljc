(ns automaton-simulation-de.simulation-engine.impl.model-test
  (:require
   #?(:clj [clojure.test :refer [deftest is]]
      :cljs [cljs.test :refer [deftest is] :include-macros true])
   [automaton-core.adapters.schema                          :as core-schema]
   [automaton-simulation-de.simulation-engine               :as-alias sim-engine]
   [automaton-simulation-de.simulation-engine.impl.model    :as sut]
   [automaton-simulation-de.simulation-engine.impl.registry :as sim-de-registry]))

(deftest schema-test (is (= nil (core-schema/validate-humanize sut/schema))))

(deftest build-test
  (is (nil? (->> (sut/build #:automaton-simulation-de.simulation-engine{} (sim-de-registry/build))
                 (core-schema/validate-data-humanize sut/schema)))
      "Minimal model with registry only is valid.")
  (is (nil? (->> (sut/build #:automaton-simulation-de.simulation-engine{} (sim-de-registry/build))
                 (core-schema/validate-data-humanize sut/schema)))
      "Default registry is valid.")
  (is (->> (sut/build {} [])
           (core-schema/validate-data-humanize sut/schema)
           :error)
      "model-data can't be an empty map, building result is not validated."))
