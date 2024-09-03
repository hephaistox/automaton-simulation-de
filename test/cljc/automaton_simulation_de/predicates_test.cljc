(ns automaton-simulation-de.predicates-test
  (:require
   [automaton-simulation-de.predicates :as sut]
   #?(:clj [clojure.test :refer [deftest is testing]]
      :cljs [cljs.test :refer [deftest is testing] :include-macros true])))

(deftest apply-predicate-query-detailed
  (is (map? (sut/apply-query-detailed [::sut/equal :color :blue]
                                      {:name :m1
                                       :color :blue}))
      "Error map is returned when predicate is not in registry")
  (is (map? (sut/apply-query-detailed [::sut/not [::sut/equal :color :blue]]
                                      {:name :m1
                                       :color :green}))
      "Error map is returned when predicate inside predicate is not in registry")
  (is (map? (sut/apply-query-detailed [::sut/equal? :color :blue :to :many :params]
                                      {:name :m1
                                       :color :green}))
      "Error map is returned when predicate params are invalid")
  (is (map? (sut/apply-query-detailed [:sut/not [::sut/equal? :color :blue :to :many :params]]
                                      {:name :m1
                                       :color :green}))
      "Error map is returned when predicate params are invalid nested")
  (is (map? (sut/apply-query-detailed [:sut/not
                                       [::sut/equal? :color :blue :to :many :params]
                                       [::sut/equal? :color :blue :to :many :params]]
                                      {:name :m1
                                       :color :green}))
      "Error map is returned when predicate params are invalid")
  (is (= "test error"
         (ex-message (:error (sut/apply-query-detailed
                              {::throw {:pred-fn (fn [& _] #(throw (ex-info "test error" {:d %})))}}
                              [::throw]
                              "whatever"))))
      "Error map is returned when predicate throws error with it's error")
  (is (= "Predicate is not matching a schema"
         (:msg (:error (sut/apply-query-detailed :pred-shouldn't-look-like-this "whatever"))))
      "Error map is returned when predicate throws error with it's error")
  (is (= "predicate does not evaluated to function"
         (:msg
          (sut/apply-query-detailed {::broken-pred {:no-pred-fn nil}} [::broken-pred] "whatever")))
      "Invalid registry causes error map to be returned"))

(deftest apply-predicate-query
  (testing "Invalid options return nil"
    (is (nil? (sut/apply-query [::sut/starts-with? :color [:wrong :type]]
                               {:name :m1
                                :color "blue"})))
    (is (nil? (sut/apply-query [::sut/equal :color :blue]
                               {:name :m1
                                :color :blue})))
    (is (nil? (sut/apply-query [::sut/not [::sut/equal :color :blue]]
                               {:name :m1
                                :color :green})))
    (is (nil? (sut/apply-query [::sut/equal? :color :blue :to :many :params]
                               {:name :m1
                                :color :green})))
    (is (nil? (sut/apply-query [:sut/not [::sut/equal? :color :blue :to :many :params]]
                               {:name :m1
                                :color :green})))
    (is (nil? (sut/apply-query [:sut/not
                                [::sut/equal? :color :blue :to :many :params]
                                [::sut/equal? :color :blue :to :many :params]]
                               {:name :m1
                                :color :green})))
    (is (nil? (sut/apply-query {::throw {:pred-fn (fn [& _]
                                                    #(throw (ex-info "test error" {:d %})))}}
                               [::throw]
                               "whatever")))
    (is (nil? (sut/apply-query {::broken-pred {:no-pred-fn nil}} [::broken-pred] "whatever"))
        "Error map is returned when predicate throws error with it's error")
    (is (nil? (sut/apply-query {::broken-pred {:no-pred-fn nil}} [::broken-pred] "whatever"))
        "Invalid registry causes error map to be returned"))
  (testing "valid queries"
    (is (true? (sut/apply-query [::sut/equal? :color :blue]
                                {:name :m1
                                 :color :blue})))
    (is (true? (sut/apply-query [::sut/not [::sut/equal? :color :blue]]
                                {:name :m1
                                 :color :green})))
    (is (false? (sut/apply-query [::sut/not [::sut/equal? :color :blue]]
                                 {:name :m1
                                  :color :blue})))
    (is (false? (sut/apply-query [::sut/equal? :color :blue]
                                 {:name :m1
                                  :color :green})))
    (is (true? (sut/apply-query [::sut/equal? :color :blue]
                                {:name :m1
                                 :color :blue})))))
