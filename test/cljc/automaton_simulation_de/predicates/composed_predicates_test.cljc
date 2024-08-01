(ns automaton-simulation-de.predicates.composed-predicates-test
  (:require
   #?(:clj [clojure.test :refer [deftest is testing]]
      :cljs [cljs.test :refer [deftest is testing] :include-macros true])
   [automaton-core.adapters.schema                         :as core-schema]
   [automaton-simulation-de.predicates                     :as sim-pred]
   [automaton-simulation-de.predicates.composed-predicates :as sut]
   [automaton-simulation-de.predicates.equality-predicates :as
                                                           sim-pred-equality]))

(deftest not-test
  (is (= false ((sut/not-fn (sim-pred-equality/is?-fn :name :m1)) {:name :m1})))
  (is (= true ((sut/not-fn (sim-pred-equality/is?-fn :name :m1)) {:name :m2})))
  (is (= false
         ((sut/not-fn (sim-pred-equality/is?-fn :name "hello"))
          {:name "hello"})))
  (is (= false
         ((sut/not-fn (sim-pred-equality/is?-fn [:name :is :deep] "hello"))
          {:name {:is {:deep "hello"}}})))
  (is (= true
         ((sut/not-fn (sim-pred-equality/is?-fn :name "hELlo"))
          {:name "hello"})))
  (is (= false ((sut/not-fn (sim-pred-equality/is?-fn :name 5)) {:name 5})))
  (is (= false ((sut/not-fn (sim-pred-equality/is?-fn 5)) 5)))
  (is (= false ((sut/not-fn (sim-pred-equality/is?-fn {:a 2})) {:a 2})))
  (is (= false ((sut/not-fn (sim-pred-equality/is?-fn "hello")) "hello"))))

(deftest and-test
  (is (= true
         ((sut/and-fn (sim-pred-equality/is?-fn :name :m1)
                      (sim-pred-equality/true?-fn :something))
          {:name :m1
           :something true})))
  (is (= false
         ((sut/and-fn (sim-pred-equality/is?-fn :name :m2)
                      (sim-pred-equality/true?-fn :something))
          {:name :m1
           :something true})))
  (is (= false
         ((sut/and-fn (sim-pred-equality/true?-fn :something)
                      (sim-pred-equality/is?-fn :name :m2))
          {:name :m1
           :something false}))))

(deftest or-test
  (is (= true
         ((sut/or-fn (sim-pred-equality/is?-fn :name :m1)
                     (sim-pred-equality/true?-fn :something))
          {:name :m1
           :something true})))
  (is (= true
         ((sut/or-fn (sim-pred-equality/is?-fn :name :m2)
                     (sim-pred-equality/true?-fn :something))
          {:name :m1
           :something true})))
  (is (= false
         ((sut/or-fn (sim-pred-equality/true?-fn :something)
                     (sim-pred-equality/is?-fn :name :m2))
          {:name :m1
           :something false}))))


(deftest registry-validation-test
  (is (true? (core-schema/validate-data sim-pred/pred-registry-schema
                                        sut/composed-predicates-lang-reg)))
  (is
   (true? (every? string?
                  (map (fn [[_ v]] (:doc v)) sut/composed-predicates-lang-reg)))
   "Generally :doc is not required for registry, but for base-lib predicates registry documentation is required"))

(defn- validation-fn
  [k]
  (get-in sut/composed-predicates-lang-reg [k :validation-fn]))

(deftest lang-validation-test
  (testing "not"
    (is (true? ((validation-fn ::sim-pred/not) [::sim-pred/not [:some :vec]])))
    (is (true? ((validation-fn ::sim-pred/not)
                [::sim-pred/not [::sim-pred/not [:is :something :lang]]])))
    (is (false? ((validation-fn ::sim-pred/not) [::sim-pred/not])))
    (is (false? ((validation-fn ::sim-pred/not)
                 [::sim-pred/not [:some :vec] [:some :vec]])))
    (is (false? ((validation-fn ::sim-pred/not)
                 [::sim-pred/not
                  [::sim-pred/not [:is :something :lang]]
                  [::sim-pred/not [:is :something :lang]]]))))
  (testing "and"
    (is (true? ((validation-fn ::sim-pred/and) [::sim-pred/and [:some :vec]])))
    (is (true? ((validation-fn ::sim-pred/and)
                [::sim-pred/and [::sim-pred/and [:is :something :lang]]])))
    (is (true? ((validation-fn ::sim-pred/and)
                [::sim-pred/and [:some :vec] [:some :vec]])))
    (is (true? ((validation-fn ::sim-pred/and)
                [::sim-pred/and
                 [::sim-pred/and [:is :something :lang]]
                 [::sim-pred/and [:is :something :lang]]])))
    (is (false? ((validation-fn ::sim-pred/and) [::sim-pred/and]))))
  (testing "or"
    (is (true? ((validation-fn ::sim-pred/or) [::sim-pred/or [:some :vec]])))
    (is (true? ((validation-fn ::sim-pred/or)
                [::sim-pred/or [::sim-pred/or [:is :something :lang]]])))
    (is (true? ((validation-fn ::sim-pred/or)
                [::sim-pred/or [:some :vec] [:some :vec]])))
    (is (true? ((validation-fn ::sim-pred/or)
                [::sim-pred/or
                 [::sim-pred/or [:is :something :lang]]
                 [::sim-pred/or [:is :something :lang]]])))
    (is (false? ((validation-fn ::sim-pred/or) [::sim-pred/or])))))
