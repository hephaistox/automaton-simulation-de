(ns automaton-simulation-de.simulation-engine.impl.stopping.cause-test
  (:require
   #?(:clj [clojure.test :refer [deftest is]]
      :cljs [cljs.test :refer [deftest is] :include-macros true])
   [automaton-core.adapters.schema
    :as core-schema]
   [automaton-simulation-de.simulation-engine
    :as-alias sim-engine]
   [automaton-simulation-de.simulation-engine.impl.stopping-definition.iteration-nth
    :as sim-de-sc-iteration-nth]
   [automaton-simulation-de.simulation-engine.impl.stopping.cause
    :as sut]))

(deftest schema-test
  (is (= nil
         (-> sut/schema
             core-schema/validate-humanize)))
  (is
   (=
    nil
    (->
      sut/schema
      (core-schema/validate-data-humanize
       #:automaton-simulation-de.simulation-engine{:stopping-criteria
                                                   #:automaton-simulation-de.simulation-engine{:params
                                                                                               {:par1
                                                                                                :a}
                                                                                               :model-end?
                                                                                               true
                                                                                               :stopping-definition
                                                                                               #:automaton-simulation-de.simulation-engine{:id
                                                                                                                                           ::sim-engine/iteration-nth
                                                                                                                                           :doc
                                                                                                                                           "doc-test"
                                                                                                                                           :next-possible?
                                                                                                                                           true
                                                                                                                                           :stopping-evaluation
                                                                                                                                           sim-de-sc-iteration-nth/stop-nth}}
                                                   :current-event
                                                   #:automaton-simulation-de.simulation-engine{:type
                                                                                               :a
                                                                                               :date
                                                                                               1}
                                                   :context {}})))))
