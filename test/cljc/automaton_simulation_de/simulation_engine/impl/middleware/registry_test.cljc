(ns automaton-simulation-de.simulation-engine.impl.middleware.registry-test
  (:require
   #?(:clj [clojure.test :refer [deftest is testing]]
      :cljs [cljs.test :refer [deftest is testing] :include-macros true])
   [automaton-core.adapters.schema                                            :as core-schema]
   [automaton-simulation-de.simulation-engine                                 :as-alias sim-engine]
   [automaton-simulation-de.simulation-engine.impl.middleware.registry        :as sut]
   [automaton-simulation-de.simulation-engine.impl.middleware.state-rendering
    :as sim-de-state-rendering]))

(deftest schema-test (is (= nil (core-schema/validate-humanize sut/schema))))

(deftest build-test (testing "Is registry valid?" (is (= nil (sut/validate (sut/build))))))

(deftest data-to-fn-test
  (testing "Non existing keywords returns nil." (is (= nil (sut/data-to-fn (sut/build) :aerz))))
  (testing "Keywords are translated to their matching function in the registry."
    (is (= sim-de-state-rendering/wrap (sut/data-to-fn (sut/build) ::sim-engine/state-rendering))))
  (testing "A vector with no other parameter is transformed to its matching function also."
    (is (= sim-de-state-rendering/wrap
           (sut/data-to-fn (sut/build) [::sim-engine/state-rendering]))))
  (testing "Doesn't modify the `:supp-middlewares-insert` keyword."
    (is (= :supp-middlewares-insert (sut/data-to-fn {} :supp-middlewares-insert))))
  (testing "A vector with a parameter is transformed to a function also."
    (is (= "print-state:,\n"
           (with-out-str (((sut/data-to-fn (sut/build)
                                           [::sim-engine/state-printing
                                            (fn [state] ["print-state:" state])])
                           (fn [_request] {:bar :foo}))
                          {}))))
    (is (= {:bar :foo}
           (((sut/data-to-fn (sut/build)
                             [::sim-engine/state-printing
                              (fn [state] (println "print-state:" state))])
             (fn [_request] {:bar :foo}))
            {}))))
  (testing "Non existing middleware is returning nil"
    (is (= nil
           (sut/data-to-fn {}
                           [:foo {}])))))
