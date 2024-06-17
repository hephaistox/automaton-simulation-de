(ns
  automaton-simulation-de.simulation-engine.impl.built-in-sd.causality-broken-test
  (:require
   #?(:clj [clojure.test :refer [deftest is]]
      :cljs [cljs.test :refer [deftest is] :include-macros true])
   [automaton-core.adapters.schema
    :as core-schema]
   [automaton-simulation-de.simulation-engine
    :as-alias sim-engine]
   [automaton-simulation-de.simulation-engine.impl.built-in-sd.causality-broken
    :as sut]
   [automaton-simulation-de.simulation-engine.impl.stopping.cause
    :as sim-de-stopping-cause]
   [automaton-simulation-de.simulation-engine.impl.stopping.definition
    :as sim-de-sc-definition]))

(deftest stopping-definition-test
  (is (= nil
         (->> sut/stopping-definition
              (core-schema/validate-data-humanize
               sim-de-sc-definition/schema)))))

(def event-stub
  #:automaton-simulation-de.simulation-engine{:type :a
                                              :date 1})

(deftest evaluates-test
  (is (= nil
         (sut/evaluates #:automaton-simulation-de.simulation-engine{:date 12}
                        #:automaton-simulation-de.simulation-engine{:date 12}
                        event-stub))
      "Same date snapshot are accepted.")
  (is (= nil
         (sut/evaluates #:automaton-simulation-de.simulation-engine{:date 12}
                        #:automaton-simulation-de.simulation-engine{:date 14}
                        event-stub))
      "Greater date snapshot are accepted.")
  (is
   (= nil
      (->> (sut/evaluates #:automaton-simulation-de.simulation-engine{:date 15}
                          #:automaton-simulation-de.simulation-engine{:date 12}
                          event-stub)
           (core-schema/validate-data-humanize sim-de-stopping-cause/schema)))
   "If next snapshot' date is smaller than current one, `causality-broken` criteria is raised."))
