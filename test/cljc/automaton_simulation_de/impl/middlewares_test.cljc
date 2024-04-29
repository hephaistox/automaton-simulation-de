(ns automaton-simulation-de.impl.middlewares-test
  (:require
   #?(:clj [clojure.test :refer [deftest is testing]]
      :cljs [cljs.test :refer [deftest is testing] :include-macros true])
   [automaton-core.adapters.schema             :as core-schema]
   [automaton-simulation-de.impl.middlewares   :as sut]
   [automaton-simulation-de.middleware.request :as sim-de-request]
   [automaton-simulation-de.scheduler.snapshot :as sim-de-snapshot]))

(deftest schema-test
  (testing "Schema" (is (nil? (core-schema/validate-humanize (sut/schema))))))

(deftest execute-middlewares-test
  (testing "Chaining of middleware is ok"
    (is
     (not
      ((sut/wrap-handler (fn [request]
                           (= {:a :b
                               :c :d}
                              request))
                         [(fn [handler]
                            (fn [request]
                              (-> request
                                  (assoc :a :b)
                                  handler)))
                          (fn [handler]
                            (fn [request]
                              (-> request
                                  (assoc :c :d)
                                  handler)))])
       (sim-de-request/build nil
                             nil
                             (sim-de-snapshot/build 2 2 2 nil nil nil)
                             nil
                             []))))
    (is
     (not
      ((sut/wrap-handler (fn [request]
                           (= {:a :b
                               :c :d}
                              request))
                         [(fn [handler]
                            (fn [request]
                              (-> request
                                  (assoc :c :d)
                                  handler)))
                          (fn [handler]
                            (fn [request]
                              (-> request
                                  (assoc :a :b)
                                  handler)))])
       (sim-de-request/build nil
                             nil
                             (sim-de-snapshot/build 2 2 2 nil nil nil)
                             nil
                             [])))))
  (testing "Middlewares can add data to the response"
    (is
     (= {:a :b
         :c :d}
        ((sut/wrap-handler (fn [_] {:a :b})
                           [(fn [handler]
                              (fn [request]
                                (-> request
                                    handler
                                    (assoc :c :d))))
                            (fn [handler]
                              (fn [request]
                                (-> request
                                    handler
                                    (assoc :a :b))))])
         (sim-de-request/build nil
                               nil
                               (sim-de-snapshot/build 2 2 2 nil nil nil)
                               nil
                               []))))
    (is
     (= {:a :b
         :c :d}
        ((sut/wrap-handler (fn [_] {:a :b})
                           [(fn [handler]
                              (fn [request]
                                (-> request
                                    handler
                                    (assoc :a :b))))
                            (fn [handler]
                              (fn [request]
                                (-> request
                                    handler
                                    (assoc :c :d))))])
         (sim-de-request/build nil
                               nil
                               (sim-de-snapshot/build 2 2 2 nil nil nil)
                               nil
                               []))))))
