#_{:heph-ignore {:forbidden-words ["tap>"]}}
(ns automaton-simulation-de.impl.middleware.tapping-test
  (:require
   [automaton-simulation-de.impl.middleware.tapping :as sut]
   #?(:clj [clojure.test :refer [deftest is]]
      :cljs [cljs.test :refer [deftest is] :include-macros true])))

(deftest wrap-response-test
  (is (= {:response true}
         (let [what-is-tapped (atom nil)]
           (with-redefs [tap> (fn [tapped-value]
                                (reset! what-is-tapped tapped-value))]
             ((sut/wrap-response (fn [_request] {:response true}))
              {:request true}))
           @what-is-tapped))))

(deftest wrap-request-test
  (is (= {:request true}
         (let [what-is-tapped (atom nil)]
           (with-redefs [tap> (fn [tapped-value]
                                (reset! what-is-tapped tapped-value))]
             ((sut/wrap-request (fn [_request] {:response true}))
              {:request true}))
           @what-is-tapped))))
