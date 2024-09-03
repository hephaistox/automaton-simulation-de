(ns automaton-simulation-de.simulation-engine-test
  (:require
   #?(:clj [clojure.test :refer [deftest is]]
      :cljs [cljs.test :refer [deftest is] :include-macros true])
   [automaton-simulation-de.simulation-engine :as sut]))

(def model-data-stub-1
  #::sut{:ordering [[::sut/field ::sut/date]]
         :future-events []
         :stopping-criterias [[::sut/iteration-nth {::sut/n 100}]]})

(deftest registries-test
  (is (= nil
         (-> (sut/registries)
             sut/validate-registry))
      "built-in registries are valid."))

(deftest validate-model-data-test
  (is (= nil (sut/validate-model-data model-data-stub-1)) "Valid model data are accepted")
  (is (some? (sut/validate-model-data (assoc model-data-stub-1 ::sut/future-events 2)))
      "Invalid model data are not accepted"))

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
  (is (= nil (sut/validate-middleware-data [] {})) "No middleware is accepted.")
  (is (= nil
         (sut/validate-middleware-data [:supp-middlewares-insert [:state-printing (constantly nil)]]
                                       {}))
      ":supp-middleware-insert is accepted, and also state-printing.")
  (is (map? (sut/validate-middleware-data :int {})) "Non vector middlewares are detected.")
  (is (map? (sut/validate-middleware-data [10 :state-printing (constantly nil)] {}))
      "Vector with invalid middlewares are detected."))

(deftest validate-stopping-criteria-data-test
  (is (some? (sut/validate-stopping-criteria-data 12 {}))
      "Stopping criteria which is not a vector is invalid.")
  (is (some? (sut/validate-stopping-criteria-data [12] {}))
      "Stopping criteria which is a vector of invalid data is invalid.")
  (is (= nil (sut/validate-stopping-criteria-data [] {})) "No stopping criteria is possible.")
  (is (= nil (sut/validate-stopping-criteria-data [:yop] {}))
      "A valid name of a stopping criteria is accepted."))

(deftest extract-snapshot-test
  (is (= nil (sut/extract-snapshot (sut/build-model model-data-stub-1 sut/registries)))
      "The result of a scheduler can be extracted to generate the next scheduler call."))

(defn- extract-stopping-causes
  [response]
  (->> response
       ::sut/stopping-causes
       (mapv #(get-in % [::sut/stopping-criteria ::sut/stopping-definition ::sut/id]))
       sort
       vec))

(defn- extract-snapshot-id [response] (get-in response [::sut/snapshot ::sut/id]))

(defn- extract-snapshot-date [response] (get-in response [::sut/snapshot ::sut/date]))

(def causes-and-snapshot-id-date-and-validation
  (juxt extract-stopping-causes extract-snapshot-id extract-snapshot-date sut/validate-response))

(deftest assembly-test
  (is
   (= [[::sut/iteration-nth] 1 0 nil]
      (-> (sut/build-model (assoc model-data-stub-1
                                  ::sut/future-events
                                  [#::sut{:type :a
                                          :date 10}]))
          (sut/scheduler [::sut/response-validation :request-validation]
                         [[::sut/iteration-nth {::sut/n 0}]])
          causes-and-snapshot-id-date-and-validation))
   "A valid model stopped at first iteration is returning one stopping cause only about `::sut/iteration-nth`, and returns the same snapshot with `id` 1 and `date` 0`")
  (is
   (= [[::sut/no-future-events] 1 0 nil]
      (-> (sut/build-model model-data-stub-1 (sut/registries))
          (sut/scheduler [::sut/response-validation :request-validation] [])
          causes-and-snapshot-id-date-and-validation))
   "When the first executed event creates no future event, then simulation stops at the second iteration with ::sim-engine/no-future-events.")
  (is (= [[::sut/iteration-nth ::sut/iteration-nth ::sut/no-future-events] 1 0 nil]
         (-> (sut/build-model model-data-stub-1 (sut/registries))
             (sut/scheduler [::sut/response-validation :request-validation]
                            [[::sut/iteration-nth {::sut/n 1}] [::sut/iteration-nth {::sut/n 1}]])
             causes-and-snapshot-id-date-and-validation))
      "When more than one stopping-criteria exists, they are all returned.")
  (is (= [[::sut/iteration-nth] 1 0 nil]
         (-> (sut/build-model (update model-data-stub-1
                                      ::sut/future-events
                                      conj
                                      {::sut/type :non-existing
                                       ::sut/date 0}))
             (sut/scheduler [::sut/response-validation :request-validation]
                            [[::sut/iteration-nth {::sut/n 0}]])
             causes-and-snapshot-id-date-and-validation))
      "If a non existing event is returned, but not executed yet, everything's fine.")
  (is (= [[::sut/execution-not-found] 2 0 nil]
         (-> (sut/build-model (update model-data-stub-1
                                      ::sut/future-events
                                      conj
                                      {::sut/type :non-existing
                                       ::sut/date 0}))
             (sut/scheduler [::sut/response-validation :request-validation]
                            [[::sut/iteration-nth {::sut/n 3}]])
             causes-and-snapshot-id-date-and-validation))
      "A non existing event trying to be executed is failing.")
  (is (= [[::sut/execution-not-found] 2 0 nil]
         (-> (sut/build-model (update model-data-stub-1
                                      ::sut/future-events
                                      conj
                                      {::sut/type :non-existing
                                       ::sut/date 0}))
             (sut/scheduler [::sut/response-validation :request-validation]
                            [[::sut/iteration-nth {::sut/n 3}]])
             causes-and-snapshot-id-date-and-validation))
      "When executed, the date of an event advances the snapshot date."))
