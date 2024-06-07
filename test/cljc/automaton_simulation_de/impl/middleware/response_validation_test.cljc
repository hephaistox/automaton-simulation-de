(ns automaton-simulation-de.impl.middleware.response-validation-test
  (:require
   #?(:clj [clojure.test :refer [deftest is testing]]
      :cljs [cljs.test :refer [deftest is testing] :include-macros true])
   [automaton-core.adapters.schema                              :as core-schema]
   [automaton-simulation-de.impl.middleware.response-validation :as sut]
   [automaton-simulation-de.impl.stopping.cause
    :as sim-de-stopping-cause]
   [automaton-simulation-de.request                             :as
                                                                sim-de-request]
   [automaton-simulation-de.response                            :as
                                                                sim-de-response]
   [automaton-simulation-de.scheduler.event                     :as
                                                                sim-de-event]
   [automaton-simulation-de.scheduler.snapshot
    :as sim-de-snapshot]))

(def evt-stub (sim-de-event/make-event :a 1))

(deftest evaluates-test
  (testing "Well form response returns `nil`."
    (is (nil? (-> (sim-de-response/build []
                                         (sim-de-snapshot/build
                                          1
                                          1
                                          1
                                          {}
                                          []
                                          (sim-de-event/make-events :a 1 :b 2)))
                  (sut/evaluates evt-stub))))
    (is (empty? (-> (sim-de-response/build []
                                           (sim-de-snapshot/build
                                            1
                                            1
                                            1
                                            {}
                                            []
                                            (sim-de-event/make-events :a 1
                                                                      :b 2)))
                    (sut/evaluates evt-stub)
                    ::sim-de-request/stopping-causes))))
  (testing "Well form response is not modifying the response."
    (is (nil? (-> (sim-de-response/build []
                                         (sim-de-snapshot/build
                                          1
                                          1
                                          1
                                          {}
                                          []
                                          (sim-de-event/make-events :a 1 :b 2)))
                  (sut/evaluates evt-stub))))
    (is (empty? (-> (sim-de-response/build []
                                           (sim-de-snapshot/build
                                            1
                                            1
                                            1
                                            {}
                                            []
                                            (sim-de-event/make-events :a 1
                                                                      :b 2)))
                    (sut/evaluates evt-stub)
                    ::sim-de-response/stopping-causes))))
  (testing
    "When detecting an issue, evaluates returns a map complying to `stopping-cause schema`."
    (is (nil? (core-schema/validate-data-humanize
               sim-de-stopping-cause/schema
               (sut/evaluates nil (sim-de-event/make-event :a 1)))))))

(deftest wrap-response-test
  (testing "Non of response."
    (is (empty? (->> nil
                     ((sut/wrap-response
                       (fn [_request]
                         (->> (sim-de-snapshot/build
                               1
                               1
                               1
                               {}
                               []
                               (sim-de-event/make-events :a 1 :b 2))
                              (sim-de-response/build [])))))
                     ::sim-de-response/stopping-causes))))
  (testing "Invalid response is detected."
    (is (seq (->> nil
                  ((sut/wrap-response (fn [_request] {:foo true})))
                  ::sim-de-response/stopping-causes)))))
