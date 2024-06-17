(ns automaton-simulation-de.simulation-engine.impl.stopping.criteria-test
  (:require
   #?(:clj [clojure.test :refer [deftest is testing]]
      :cljs [cljs.test :refer [deftest is testing] :include-macros true])
   [automaton-core.adapters.schema
    :as core-schema]
   [automaton-simulation-de.simulation-engine
    :as-alias sim-engine]
   [automaton-simulation-de.simulation-engine.impl.stopping-definition.now
    :as sim-de-sc-now]
   [automaton-simulation-de.simulation-engine.impl.stopping.criteria
    :as sut]))

(deftest schema-test (is (nil? (core-schema/validate-humanize sut/schema))))

(deftest evaluates-test
  (is (= nil (sut/evaluates nil nil)) "Invalid stopping-definition is skipped")
  (is
   (some?
    (->
      #:automaton-simulation-de.simulation-engine{:params {:par1 :a}
                                                  :stopping-definition
                                                  #:automaton-simulation-de.simulation-engine{:id
                                                                                              ::sim-engine/stop-now
                                                                                              :built-in?
                                                                                              true
                                                                                              :next-possible?
                                                                                              true
                                                                                              :doc
                                                                                              "doc-test"
                                                                                              :stopping-evaluation
                                                                                              sim-de-sc-now/stop-now}}
      (sut/evaluates #:automaton-simulation-de.simulation-engine{:id 1
                                                                 :iteration 1
                                                                 :date 1
                                                                 :state {}
                                                                 :past-events []
                                                                 :future-events
                                                                 []})
      ::sim-engine/stopping-criteria))
   "Stopping criteria `stop-now` returns a stopping criteria"))

(deftest out-of-model-test
  (is (= #:automaton-simulation-de.simulation-engine{:model-end? false}
         (sut/out-of-model nil)))
  (is (= #:automaton-simulation-de.simulation-engine{:model-end? true}
         (sut/model-end nil))))

(deftest api-data-to-entity-test
  (testing "Wrong type is skipped."
    (is (nil? (sut/api-data-to-entity nil {})))
    (is (nil? (sut/api-data-to-entity nil [:bad 1 {:foo :bar}]))))
  (testing "Non existing keywords in the registry returns nil."
    (is (nil? (sut/api-data-to-entity {} :a)))
    (is (nil? (sut/api-data-to-entity {} [:a]))))
  (is (= #:automaton-simulation-de.simulation-engine{:params {}
                                                     :stopping-definition
                                                     {:definition :stub}}
         (sut/api-data-to-entity {:good {:definition :stub}} :good))
      "Keyword is understood as a stopping-criteria with no params.")
  (is (= #:automaton-simulation-de.simulation-engine{:params {:foo :bar}
                                                     :stopping-definition
                                                     {:definition :stub}}
         (sut/api-data-to-entity {:good {:definition :stub}}
                                 [:good {:foo :bar}]))
      "A vector of keyword and map is turned into a stopping-criteria"))
