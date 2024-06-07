(ns automaton-simulation-de.core-test
  (:require
   [automaton-simulation-de.core                 :as sut]
   #?(:clj [clojure.test :refer [deftest is testing]]
      :cljs [cljs.test :refer [deftest is testing] :include-macros true])
   [automaton-simulation-de.event-library.common :as sim-de-common]
   [automaton-simulation-de.response             :as sim-de-response]
   [automaton-simulation-de.scheduler.event      :as sim-de-event]
   [automaton-simulation-de.scheduler.snapshot   :as sim-de-snapshot]))

(def model-data-stub-1
  {:initial-event-type :IN
   :ordering [[:field ::sim-de-event/date]]
   :stopping-criterias [[:iteration-nth {:n 100}]]})

(deftest registries-test
  (testing "built-in registries are valid."
    (is (-> (sut/registries)
            sut/validate-registry
            nil?))))

(deftest validate-model-data-test
  (testing "Valid model data are accepted"
    (is (nil? (sut/validate-model-data model-data-stub-1))))
  (testing "Invalid model data are not accepted"
    (is (some? (sut/validate-model-data (dissoc model-data-stub-1
                                         :initial-event-type))))))

(deftest build-model-test
  (is (->> [(sut/build-model nil nil)]
           (mapv sut/validate-model)
           (every? map?))
      "Invalid models are detected")
  (is (empty? (->> [(sut/build-model model-data-stub-1 (sut/registries))]
                   (map sut/validate-model)
                   (filter some?)))
      "Models build with `build-model` are accepted"))

(deftest validate-middleware-data-test
  (testing "Accepted middlewares."
    (is (nil? (sut/validate-middleware-data [] {})))
    (is (nil? (sut/validate-middleware-data [:supp-middlewares-insert
                                             [:state-printing (constantly nil)]]
                                            {}))))
  (testing "Invalid middlewares are detected."
    (is (map? (sut/validate-middleware-data :int {})))
    (is (map? (sut/validate-middleware-data
               [10 :state-printing (constantly nil)]
               {})))))

(deftest validate-stopping-criteria-data-test
  (testing "Invalid stopping raise errors."
    (is (some? (sut/validate-stopping-criteria-data 12 {})))
    (is (some? (sut/validate-stopping-criteria-data [12] {}))))
  (testing "Valid stopping criteria."
    (is (nil? (sut/validate-stopping-criteria-data [] {})))
    (is (nil? (sut/validate-stopping-criteria-data [:yop] {})))))

(deftest extract-snapshot-test
  (is
   (nil? (sut/extract-snapshot (sut/build-model model-data-stub-1
                                                sut/registries)))
   "The result of a scheduler can be extracted to generate the next scheduler call."))

(defn- extract-stopping-causes
  [response]
  (->> response
       ::sim-de-response/stopping-causes
       (mapv #(get-in % [:stopping-criteria :stopping-definition :id]))
       sort
       vec))

(defn- extract-snapshot-id
  [response]
  (get-in response [::sim-de-response/snapshot ::sim-de-snapshot/id]))

(defn- extract-snapshot-date
  [response]
  (get-in response [::sim-de-response/snapshot ::sim-de-snapshot/date]))

(def causes-and-snapshot-id-date-and-validation
  (juxt extract-stopping-causes
        extract-snapshot-id
        extract-snapshot-date
        sut/validate-response))

(deftest assembly-test
  (is
   (= [[:iteration-nth] 1 0 nil]
      (-> (sut/build-model model-data-stub-1)
          (sut/scheduler [:response-validation :request-validation]
                         [[:iteration-nth {:n 0}]])
          causes-and-snapshot-id-date-and-validation))
   "A valid model stopped at first iteration is returning one stopping cause only about `:iteration-nth`, and returns the same snapshot with `id` 1 and `date` 0`")
  (is
   (= [[:no-future-events] 2 0 nil]
      (-> (sut/build-model model-data-stub-1
                           (-> (sut/registries)
                               (assoc-in [:event :IN]
                                         (sim-de-common/init-events [] 0))))
          (sut/scheduler [:response-validation :request-validation] [])
          causes-and-snapshot-id-date-and-validation))
   "When the first executed event creates no future event, then simulation stops at the second iteration with :no-future-events.")
  (is (= [[:iteration-nth :iteration-nth :no-future-events] 2 0 nil]
         (-> (sut/build-model model-data-stub-1
                              (-> (sut/registries)
                                  (assoc-in [:event :IN]
                                            (sim-de-common/init-events [] 0))))
             (sut/scheduler [:response-validation :request-validation]
                            [[:iteration-nth {:n 2}] [:iteration-nth {:n 2}]])
             causes-and-snapshot-id-date-and-validation))
      "When more than one stopping-criteria exists, they are all returned.")
  (is
   (= [[:iteration-nth] 2 0 nil]
      (-> (sut/build-model model-data-stub-1
                           (-> (sut/registries)
                               (assoc-in [:event :IN]
                                         (sim-de-common/init-events
                                          [{::sim-de-event/type :non-existing}]
                                          1))))
          (sut/scheduler [:response-validation :request-validation]
                         [[:iteration-nth {:n 2}]])
          causes-and-snapshot-id-date-and-validation))
   "If a non existing event is returned, but not executed yet, everything's fine.")
  (is (= [[:execution-not-found] 3 0 nil]
         (-> (sut/build-model model-data-stub-1
                              (-> (sut/registries)
                                  (assoc-in [:event :IN]
                                            (sim-de-common/init-events
                                             [{::sim-de-event/type
                                               :non-existing}]
                                             0))))
             (sut/scheduler [:response-validation :request-validation]
                            [[:iteration-nth {:n 3}]])
             causes-and-snapshot-id-date-and-validation))
      "A non existing event trying to be executed is failing.")
  (is (= [[:execution-not-found] 3 1 nil]
         (-> (sut/build-model model-data-stub-1
                              (-> (sut/registries)
                                  (assoc-in [:event :IN]
                                            (sim-de-common/init-events
                                             [{::sim-de-event/type
                                               :non-existing}]
                                             1))))
             (sut/scheduler [:response-validation :request-validation]
                            [[:iteration-nth {:n 3}]])
             causes-and-snapshot-id-date-and-validation))
      "When executed, the date of an event advances the snapshot date."))
