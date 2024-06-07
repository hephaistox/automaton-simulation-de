(ns automaton-simulation-de.impl.stopping.cause-test
  (:require
   #?(:clj [clojure.test :refer [deftest is]]
      :cljs [cljs.test :refer [deftest is] :include-macros true])
   [automaton-core.adapters.schema                                 :as
                                                                   core-schema]
   [automaton-simulation-de.impl.stopping-definition.iteration-nth
    :as sim-de-sc-iteration-nth]
   [automaton-simulation-de.impl.stopping.cause                    :as sut]
   [automaton-simulation-de.scheduler.event
    :as sim-de-event]))

(deftest schema-test
  (is (= nil
         (-> sut/schema
             core-schema/validate-humanize)))
  (is (= nil
         (-> sut/schema
             (core-schema/validate-data-humanize
              {:stopping-criteria {:params {:par1 :a}
                                   :model-end? true
                                   :stopping-definition
                                   {:id :iteration-nth
                                    :doc "doc-test"
                                    :next-possible? true
                                    :stopping-evaluation
                                    sim-de-sc-iteration-nth/stop-nth}}
               :current-event (sim-de-event/make-event :a 1)
               :context {}})))))
