(ns automaton-simulation-de.predicates.equality-predicates-test
  (:require
   [automaton-core.adapters.schema                         :as core-schema]
   [automaton-simulation-de.predicates                     :as sim-pred]
   #?(:clj [clojure.test :refer [deftest is testing]]
      :cljs [cljs.test :refer [deftest is testing] :include-macros true])
   [automaton-simulation-de.predicates.equality-predicates :as sut]))


(deftest is?-fn-test
  (is (true? ((sut/is?-fn :name :m1) {:name :m1})))
  (is (false? ((sut/is?-fn :name :m1) {:name :m2})))
  (is (true? ((sut/is?-fn :name "hello") {:name "hello"})))
  (is (true? ((sut/is?-fn [:name :is :deep] "hello") {:name {:is {:deep "hello"}}})))
  (is (false? ((sut/is?-fn :name "hELlo") {:name "hello"})))
  (is (true? ((sut/is?-fn :name 5) {:name 5})))
  (is (true? ((sut/is?-fn 5) 5)))
  (is (true? ((sut/is?-fn {:a 2}) {:a 2})))
  (is (true? ((sut/is?-fn "hello") "hello"))))

(deftest one-of?-fn-test
  (is (true? ((sut/one-of?-fn :name [:m1]) {:name :m1})))
  (is (true? ((sut/one-of?-fn :name [:m1 :m2]) {:name :m2})))
  (is (false? ((sut/one-of?-fn :name [:m1 :m2]) {:name :m3})))
  (is (true? ((sut/one-of?-fn :name ["hello"]) {:name "hello"})))
  (is (false? ((sut/one-of?-fn :name ["hello"]) {:name "hellobro"})))
  (is (true? ((sut/one-of?-fn [:name :is :deep] ["hello"]) {:name {:is {:deep "hello"}}})))
  (is (false? ((sut/one-of?-fn :name ["hELlo"]) {:name "hello"})))
  (is (true? ((sut/one-of?-fn :name (range 1 10)) {:name 5})))
  (is (true? ((sut/one-of?-fn :name (range 1 10)) {:name 2})))
  (is (true? ((sut/one-of?-fn :name (range 1 10)) {:name 9})))
  (is (true? ((sut/one-of?-fn [5]) 5)))
  (is (nil? ((sut/one-of?-fn :wrong) :wrong)))
  (is (nil? ((sut/one-of?-fn "hello") "hello")))
  (is (nil? ((sut/one-of?-fn "hello") "h"))))

(deftest is-empty?-fn-test
  (is (true? ((sut/is-empty?-fn :name) {:name nil})))
  (is (true? ((sut/is-empty?-fn :name) {:name []})))
  (is (true? ((sut/is-empty?-fn :name) {:name {}})))
  (is (true? ((sut/is-empty?-fn :name) {:name '()})))
  (is (true? ((sut/is-empty?-fn [:a :b :c]) {:name {:a {:b 5}}})))
  (is (true? ((sut/is-empty?-fn) {})))
  (is (true? ((sut/is-empty?-fn) nil)))
  (is (false? ((sut/is-empty?-fn :name) {:name 5})))
  (is (false? ((sut/is-empty?-fn :name) {:name [1]})))
  (is (false? ((sut/is-empty?-fn :name) {:name {:a 2}})))
  (is (false? ((sut/is-empty?-fn :name) {:name '(3)})))
  (is (false? ((sut/is-empty?-fn [:name :a :b :c]) {:name {:a {:b {:c 8}}}})))
  (is (false? ((sut/is-empty?-fn) 5))))

(deftest contains?-fn-test
  (testing "string"
    (is (true? ((sut/contains?-fn :name "MX") {:name "AMX500"})))
    (is (false? ((sut/contains?-fn :name "MX") {:name "IDon'tHaveIt"})))
    (is (true? ((sut/contains?-fn :name "MX") {:name "MX-Starts with"})))
    (is (true? ((sut/contains?-fn :name "MX") {:name "Ends with MX"})))
    (is (true? ((sut/contains?-fn [:name :what] "MX") {:name {:what "Ends with MX"}})))
    (is (true? ((sut/contains?-fn "MX") "MX"))))
  (testing "vector"
    (is (true? ((sut/contains?-fn :machines :m1) {:machines [:m1 :m2 :m3]})))
    (is (true? ((sut/contains?-fn :machines :m1) {:machines [:m1]})))
    (is (true? ((sut/contains?-fn :machines :m1) {:machines [:m2 :m3 :m1]})))
    (is (true? ((sut/contains?-fn [:machines :deep] :m1) {:machines {:deep [:m2 :m3 :m1]}})))
    (is (true? ((sut/contains?-fn :m1) [:m2 :m3 :m1])))
    (is (false? ((sut/contains?-fn :m1) [:m2 :m3])))
    (is (false? ((sut/contains?-fn :m1) [])))
    (is (false? ((sut/contains?-fn :m1) [[:m1]])))
    (is (false? ((sut/contains?-fn :non-existing :m1) [:m1]))))
  (testing "list"
    (is (true? ((sut/contains?-fn :machines :m1) {:machines '(:m1 :m2 :m3)})))
    (is (true? ((sut/contains?-fn :machines :m1) {:machines '(:m1)})))
    (is (true? ((sut/contains?-fn :machines :m1) {:machines '(:m2 :m3 :m1)})))
    (is (true? ((sut/contains?-fn '(:machines :deep) :m1) {:machines {:deep '(:m2 :m3 :m1)}})))
    (is (true? ((sut/contains?-fn :m1) '(:m2 :m3 :m1))))
    (is (false? ((sut/contains?-fn :m1) '(:m2 :m3))))
    (is (false? ((sut/contains?-fn :m1) '())))
    (is (false? ((sut/contains?-fn :m1) '([:m1]))))
    (is (false? ((sut/contains?-fn :non-existing :m1) '(:m1))))))

(deftest starts-with?-fn
  (is (false? ((sut/starts-with?-fn :name "MX") {:name "AMX500"})))
  (is (false? ((sut/starts-with?-fn :name "MX") {:name "IDon'tHaveIt"})))
  (is (true? ((sut/starts-with?-fn :name "MX") {:name "MX-Starts with"})))
  (is (false? ((sut/starts-with?-fn :name "MX") {:name "Ends with MX"})))
  (is (true? ((sut/starts-with?-fn [:name :what] "MX") {:name {:what "MX5000"}})))
  (is (true? ((sut/starts-with?-fn "MX") "MX")))
  (is (nil? ((sut/starts-with?-fn "MX") 5))))


(deftest ends-with?-fn
  (is (false? ((sut/ends-with?-fn :name "MX") {:name "AMX500"})))
  (is (false? ((sut/ends-with?-fn :name "MX") {:name "IDon'tHaveIt"})))
  (is (false? ((sut/ends-with?-fn :name "MX") {:name "MX-Starts with"})))
  (is (true? ((sut/ends-with?-fn :name "MX") {:name "Ends with MX"})))
  (is (false? ((sut/ends-with?-fn [:name :what] "MX") {:name {:what "MX5000"}})))
  (is (true? ((sut/ends-with?-fn "MX") "MX")))
  (is (nil? ((sut/ends-with?-fn "MX") 5))))

(deftest true?-fn-test
  (is (true? ((sut/true?-fn :on) {:on true})))
  (is (false? ((sut/true?-fn :on) {:on false})))
  (is (false? ((sut/true?-fn :on) {:on nil})))
  (is (false? ((sut/true?-fn :on) {:whatever true})))
  (is (true? ((sut/true?-fn) true)))
  (is (false? ((sut/true?-fn) :whatever))))

(deftest false?-fn-test
  (is (false? ((sut/false?-fn :on) {:on true})))
  (is (true? ((sut/false?-fn :on) {:on false})))
  (is (false? ((sut/false?-fn :on) {:on nil})))
  (is (false? ((sut/false?-fn :on) {:whatever true})))
  (is (false? ((sut/false?-fn) true)))
  (is (false? ((sut/false?-fn) :whatever))))

(deftest registry-validation-test
  (is (true? (core-schema/validate-data sim-pred/pred-registry-schema
                                        sut/equality-predicates-lang-reg)))
  (is
   (true? (every? string? (map (fn [[_ v]] (:doc v)) sut/equality-predicates-lang-reg)))
   "Generally :doc is not required for registry, but for base-lib predicates registry documentation is required"))

(defn- validation-fn [k] (get-in sut/equality-predicates-lang-reg [k :validation-fn]))

(deftest lang-validation-test
  (testing "equal?"
    (is (true? ((validation-fn ::sim-pred/equal?) [::sim-pred/equal? :name :value])))
    (is (true? ((validation-fn ::sim-pred/equal?) [::sim-pred/equal? "name" :value])))
    (is (true? ((validation-fn ::sim-pred/equal?) [::sim-pred/equal? [:long :path :name] :value])))
    (is (true? ((validation-fn ::sim-pred/equal?) [::sim-pred/equal? [:long :path "name"] :value])))
    (is (true? ((validation-fn ::sim-pred/equal?) [::sim-pred/equal? :value])))
    (is (false? ((validation-fn ::sim-pred/equal?) [::sim-pred/equal?])))
    (is (false? ((validation-fn ::sim-pred/equal?) [::sim-pred/equal? :to :many :arguments]))))
  (testing "one-of?"
    (is (true? ((validation-fn ::sim-pred/one-of?) [::sim-pred/one-of? :name [:value :or :more]])))
    (is (true? ((validation-fn ::sim-pred/one-of?)
                [::sim-pred/one-of? "name" [:value "different" 5]])))
    (is (true? ((validation-fn ::sim-pred/one-of?) [::sim-pred/one-of? [:long :path :name] []])))
    (is (true? ((validation-fn ::sim-pred/one-of?) [::sim-pred/one-of? [:long :path "name"] '()])))
    (is (true? ((validation-fn ::sim-pred/one-of?) [::sim-pred/one-of? ["values" "to" "compare"]])))
    (is (false? ((validation-fn ::sim-pred/one-of?) [::sim-pred/one-of?])))
    (is (false? ((validation-fn ::sim-pred/one-of?) [::sim-pred/one-of? :to :many :arguments])))
    (is (false? ((validation-fn ::sim-pred/one-of?) [::sim-pred/one-of? :path :wrong-value])))
    (is (false? ((validation-fn ::sim-pred/one-of?)
                 [::sim-pred/one-of? :path "I dont work with strings"])))
    (is (false? ((validation-fn ::sim-pred/one-of?) [::sim-pred/one-of? :path 5]))))
  (testing "is-empty?"
    (is (true? ((validation-fn ::sim-pred/is-empty?) [::sim-pred/is-empty? :name])))
    (is (true? ((validation-fn ::sim-pred/is-empty?) [::sim-pred/is-empty? "name"])))
    (is (true? ((validation-fn ::sim-pred/is-empty?) [::sim-pred/is-empty? [:long :path :name]])))
    (is (true? ((validation-fn ::sim-pred/is-empty?) [::sim-pred/is-empty? [:long :path "name"]])))
    (is (true? ((validation-fn ::sim-pred/is-empty?) [::sim-pred/is-empty?])))
    (is (false? ((validation-fn ::sim-pred/is-empty?) [::sim-pred/is-empty? :to :many :arguments])))
    (is (false? ((validation-fn ::sim-pred/is-empty?) [::sim-pred/is-empty? :path :to-many]))))
  (testing "contains?"
    (is (true? ((validation-fn ::sim-pred/contains?) [::sim-pred/contains? :name "val"])))
    (is (true? ((validation-fn ::sim-pred/contains?) [::sim-pred/contains? "name" "val"])))
    (is (true? ((validation-fn ::sim-pred/contains?)
                [::sim-pred/contains? [:long :path :name] "val"])))
    (is (true? ((validation-fn ::sim-pred/contains?)
                [::sim-pred/contains? [:long :path "name"] [:vals :more :some]])))
    (is (true? ((validation-fn ::sim-pred/contains?) [::sim-pred/contains? :name '("val" 2)])))
    (is (true? ((validation-fn ::sim-pred/contains?) [::sim-pred/contains? :name 5])))
    (is (true? ((validation-fn ::sim-pred/contains?) [::sim-pred/contains? "just value"])))
    (is (false? ((validation-fn ::sim-pred/contains?) [::sim-pred/contains?])))
    (is (false? ((validation-fn ::sim-pred/contains?)
                 [::sim-pred/contains? :to :many :arguments]))))
  (testing "starts-with?"
    (is (true? ((validation-fn ::sim-pred/starts-with?) [::sim-pred/starts-with? :name "val"])))
    (is (true? ((validation-fn ::sim-pred/starts-with?) [::sim-pred/starts-with? "name" "val"])))
    (is (true? ((validation-fn ::sim-pred/starts-with?)
                [::sim-pred/starts-with? [:long :path :name] "val"])))
    (is (true? ((validation-fn ::sim-pred/starts-with?) [::sim-pred/starts-with? "just value"])))
    (is (false? ((validation-fn ::sim-pred/starts-with?)
                 [::sim-pred/starts-with? :path [:vals :more :some]])))
    (is (false? ((validation-fn ::sim-pred/starts-with?)
                 [::sim-pred/starts-with? :name '("val" 2)])))
    (is (false? ((validation-fn ::sim-pred/starts-with?) [::sim-pred/starts-with? :name 5])))
    (is (false? ((validation-fn ::sim-pred/starts-with?) [::sim-pred/starts-with?])))
    (is (false? ((validation-fn ::sim-pred/starts-with?)
                 [::sim-pred/starts-with? :to "many" "args"]))))
  (testing "ends-with?"
    (is (true? ((validation-fn ::sim-pred/ends-with?) [::sim-pred/ends-with? :name "val"])))
    (is (true? ((validation-fn ::sim-pred/ends-with?) [::sim-pred/ends-with? "name" "val"])))
    (is (true? ((validation-fn ::sim-pred/ends-with?)
                [::sim-pred/ends-with? [:long :path :name] "val"])))
    (is (true? ((validation-fn ::sim-pred/ends-with?) [::sim-pred/ends-with? "just value"])))
    (is (false? ((validation-fn ::sim-pred/ends-with?)
                 [::sim-pred/ends-with? :path [:vals :more :some]])))
    (is (false? ((validation-fn ::sim-pred/ends-with?) [::sim-pred/ends-with? :name '("val" 2)])))
    (is (false? ((validation-fn ::sim-pred/ends-with?) [::sim-pred/ends-with? :name 5])))
    (is (false? ((validation-fn ::sim-pred/ends-with?) [::sim-pred/ends-with?])))
    (is (false? ((validation-fn ::sim-pred/ends-with?) [::sim-pred/ends-with? :to "many" "args"]))))
  (testing "true?"
    (is (true? ((validation-fn ::sim-pred/true?) [::sim-pred/true? :name])))
    (is (true? ((validation-fn ::sim-pred/true?) [::sim-pred/true? "name"])))
    (is (true? ((validation-fn ::sim-pred/true?) [::sim-pred/true? [:long :path :name]])))
    (is (true? ((validation-fn ::sim-pred/true?) [::sim-pred/true? [:long :path "name"]])))
    (is (true? ((validation-fn ::sim-pred/true?) [::sim-pred/true?])))
    (is (false? ((validation-fn ::sim-pred/true?) [::sim-pred/true? :to :many :arguments])))
    (is (false? ((validation-fn ::sim-pred/true?) [::sim-pred/true? :path :to-many]))))
  (testing "false?"
    (is (true? ((validation-fn ::sim-pred/false?) [::sim-pred/false? :name])))
    (is (true? ((validation-fn ::sim-pred/false?) [::sim-pred/false? "name"])))
    (is (true? ((validation-fn ::sim-pred/false?) [::sim-pred/false? [:long :path :name]])))
    (is (true? ((validation-fn ::sim-pred/false?) [::sim-pred/false? [:long :path "name"]])))
    (is (true? ((validation-fn ::sim-pred/false?) [::sim-pred/false?])))
    (is (false? ((validation-fn ::sim-pred/false?) [::sim-pred/false? :to :many :arguments])))
    (is (false? ((validation-fn ::sim-pred/false?) [::sim-pred/false? :path :to-many])))))
