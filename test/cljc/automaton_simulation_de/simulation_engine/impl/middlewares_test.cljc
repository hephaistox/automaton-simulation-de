(ns automaton-simulation-de.simulation-engine.impl.middlewares-test
  (:require
   #?(:clj [clojure.test :refer [deftest is testing]]
      :cljs [cljs.test :refer [deftest is testing] :include-macros true])
   [automaton-core.adapters.schema                             :as core-schema]
   [automaton-simulation-de.simulation-engine                  :as-alias
                                                               sim-engine]
   [automaton-simulation-de.simulation-engine.impl.middlewares :as sut]))

(deftest schema-test (is (= nil (core-schema/validate-humanize sut/schema))))

(def ^:private snapshot-stub
  #:automaton-simulation-de.simulation-engine{:id 2
                                              :iteration 2
                                              :date 2
                                              :state nil
                                              :past-events nil
                                              :future-events nil})

(def ^:private request-stub
  #:automaton-simulation-de.simulation-engine{:current-event nil
                                              :event-execution nil
                                              :snapshot snapshot-stub
                                              :sorting (constantly nil)
                                              :stopping-causes nil})

(deftest wrap-handler-test
  (testing "Chaining of middleware is ok"
    (is (not ((sut/wrap-handler (fn [request]
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
              request-stub)))
    (is (not ((sut/wrap-handler (fn [request]
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
              request-stub))))
  (testing "Middlewares can add data to the response"
    (is (= {:a :b
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
            request-stub)))
    (is (= {:a :b
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
            request-stub)))))
