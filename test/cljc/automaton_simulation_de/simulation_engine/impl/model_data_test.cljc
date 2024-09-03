(ns automaton-simulation-de.simulation-engine.impl.model-data-test
  (:require
   #?(:clj [clojure.test :refer [deftest is testing]]
      :cljs [cljs.test :refer [deftest is testing] :include-macros true])
   [automaton-core.adapters.schema                            :as core-schema]
   [automaton-simulation-de.simulation-engine                 :as-alias sim-engine]
   [automaton-simulation-de.simulation-engine.impl.model-data :as sut]))

(deftest schema-test
  (testing "Schema are valid."
    (is (= nil (core-schema/validate-humanize sut/middlewares-schema)))
    (is (= nil (core-schema/validate-humanize sut/stopping-criterias-schema)))
    (is (= nil (core-schema/validate-humanize sut/ordering-schema)))
    (is (= nil (core-schema/validate-humanize sut/schema)))))

(deftest middlewares-schema-test
  (testing "Schema is valid." (is (= nil (core-schema/validate-humanize sut/middlewares-schema))))
  (testing "Empty middlewares are ok."
    (is (= nil (core-schema/validate-data-humanize sut/middlewares-schema []))))
  (testing "Keyword middlewares are ok."
    (is (= nil (core-schema/validate-data-humanize sut/middlewares-schema [:foo :bar]))))
  (testing "Vectors of keyword middlewares are ok."
    (is (= nil (core-schema/validate-data-humanize sut/middlewares-schema [[:foo]]))))
  (testing "Vectors of keyword + maps are ok."
    (is (= nil
           (core-schema/validate-data-humanize sut/middlewares-schema
                                               [[:foo {:bar 1}]]))))
  (testing "Mixed vectors are ok."
    (is (= nil
           (core-schema/validate-data-humanize sut/middlewares-schema
                                               [[:foo {:bar 1}] :bar [:a]]))))
  (testing "Malformed vectors are ok."
    (is (some? (core-schema/validate-data-humanize sut/middlewares-schema 12))))
  (testing "Middleware that contain fn are ok"
    (is (= nil
           (core-schema/validate-data-humanize
            sut/middlewares-schema
            [[10 :state-printing (fn [handler] (fn [request] (request handler)))]])))))

(deftest stopping-criteria-schema-test
  (testing "Valid stopping criteria."
    (is (= nil (core-schema/validate-data-humanize sut/stopping-criterias-schema [[:foo]])))
    (is (= nil
           (core-schema/validate-data-humanize sut/stopping-criterias-schema
                                               [[:foo {}]])))
    (is (= nil (core-schema/validate-data-humanize sut/stopping-criterias-schema [])))
    (is
     (=
      ["invalid type"]
      (->
        (core-schema/validate-data-humanize
         sut/stopping-criterias-schema
         #:automaton-simulation-de.simulation-engine{:model-end? false
                                                     :params {:whatever "whenever"}
                                                     :stopping-definition
                                                     #:automaton-simulation-de.simulation-engine{:doc
                                                                                                 "test"
                                                                                                 :id
                                                                                                 :test-one
                                                                                                 :next-possible?
                                                                                                 false}})
        :error))))
  (deftest ordering-schema-test
    (testing "Invalid ordering are rejected"
      (is (= [[["should be :automaton-simulation-de.simulation-engine/field"
                "should be :automaton-simulation-de.simulation-engine/type"]
               ["should be a keyword"]]]
             (-> (core-schema/validate-data-humanize sut/ordering-schema
                                                     [[:yop [:machine :product]]])
                 :error)))
      (is (= [[["should be :automaton-simulation-de.simulation-engine/field"]
               ["should be a keyword" "invalid type"]]]
             (-> (core-schema/validate-data-humanize sut/ordering-schema [[::sim-engine/type 12]])
                 :error))))
    (testing "Valid ordering are accepted."
      (is (= nil (core-schema/validate-data-humanize sut/ordering-schema [])))
      (is (= nil
             (core-schema/validate-data-humanize sut/ordering-schema
                                                 [[::sim-engine/type [:machine :product]]])))
      (is (= nil
             (core-schema/validate-data-humanize sut/ordering-schema
                                                 [[::sim-engine/field :machine]]))))))
