(ns automaton-simulation-de.impl.stopping.criteria-test
  (:require
   #?(:clj [clojure.test :refer [deftest is testing]]
      :cljs [cljs.test :refer [deftest is testing] :include-macros true])
   [automaton-core.adapters.schema                       :as core-schema]
   [automaton-simulation-de.impl.stopping-definition.now :as sim-de-sc-now]
   [automaton-simulation-de.impl.stopping.criteria       :as sut]
   [automaton-simulation-de.scheduler.snapshot           :as sim-de-snapshot]))

(deftest schema-test (is (nil? (core-schema/validate-humanize sut/schema))))

(deftest evaluates-test
  (testing "Invalid stopping-definition is skipped"
    (is (-> (sut/evaluates nil (sim-de-snapshot/build 1 1 10 {} [] []))
            nil?)))
  (testing "Stopping criteria `stop-now` returns stop."
    (is (some? (-> {:params {:par1 :a}
                    :stopping-definition {:id :stop-now
                                          :built-in? true
                                          :next-possible? true
                                          :doc "doc-test"
                                          :stopping-evaluation
                                          sim-de-sc-now/stop-now}}
                   (sut/evaluates (sim-de-snapshot/build 1 1 10 {} [] [])))))))

(deftest out-of-model-test
  (is (= {:model-end? false} (sut/out-of-model nil)))
  (is (= {:model-end? true} (sut/model-end nil))))

(deftest api-data-to-entity-test
  (testing "Wrong type is skipped."
    (is (nil? (sut/api-data-to-entity nil {})))
    (is (nil? (sut/api-data-to-entity nil [:bad 1 {:foo :bar}]))))
  (testing "Non existing keywords in the registry returns nil."
    (is (nil? (sut/api-data-to-entity {} :a)))
    (is (nil? (sut/api-data-to-entity {} [:a]))))
  (testing "Keyword is understood as a stopping-criteria with no params."
    (is (= {:params {}
            :stopping-definition {:definition :stub}}
           (sut/api-data-to-entity {:good {:definition :stub}} :good))))
  (is (= {:params {:foo :bar}
          :stopping-definition {:definition :stub}}
         (sut/api-data-to-entity {:good {:definition :stub}}
                                 [:good {:foo :bar}]))))
