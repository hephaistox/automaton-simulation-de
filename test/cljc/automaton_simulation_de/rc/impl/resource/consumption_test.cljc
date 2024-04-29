(ns automaton-simulation-de.rc.impl.resource.consumption-test
  (:require
   [automaton-simulation-de.rc                           :as sim-de-rc]
   [automaton-simulation-de.rc.impl.resource.consumption :as sut]
   #?(:clj [clojure.test :refer [deftest is testing]]
      :cljs [cljs.test :refer [deftest is testing] :include-macros true])))

(deftest consume-test
  (testing "Incomplete events are not updated"
    (is (= [nil nil] (sut/consume nil nil nil)))
    (is (= [nil {}] (sut/consume {} nil nil)))
    (is (= [nil {:a :b}] (sut/consume {:a :b} 1 nil))))
  (testing "Seizing are stored with a uuid"
    (is (uuid? (-> (sut/consume {} 1 {:a :b})
                   first)))
    (is (uuid? (-> (sut/consume {} 1 {:a :b})
                   second
                   ::sim-de-rc/currently-consuming
                   ffirst)))
    (is (= {:a :b}
           (let [[consumption-uuid resource] (sut/consume {} 1 {:a :b})]
             (get-in resource
                     [::sim-de-rc/currently-consuming
                      consumption-uuid
                      ::sim-de-rc/seizing-event])))))
  (testing "Seizing are storing event and its quantity"
    (is (= #::sim-de-rc{:seizing-event {:a :b}
                        :consumed-quantity 1}
           (let [[consumption-uuid resource] (sut/consume {} 1 {:a :b})]
             (get-in resource
                     [::sim-de-rc/currently-consuming consumption-uuid]))))
    (is (= #::sim-de-rc{:seizing-event {:a :b}
                        :consumed-quantity 3}
           (let [[consumption-uuid resource] (sut/consume {} 3 {:a :b})]
             (get-in resource
                     [::sim-de-rc/currently-consuming consumption-uuid]))))))

(deftest free-test
  (testing "A removed event is not in the list anymore"
    (is (= #::sim-de-rc{:currently-consuming {}}
           (sut/free #::sim-de-rc{:currently-consuming
                                  {#uuid "33497220-f844-11ee-9fa1-17acea14e9df"
                                   #::sim-de-rc{:event {:a :b}
                                                :consumed-quantity 3}}}
                     #uuid "33497220-f844-11ee-9fa1-17acea14e9df"))))
  (testing "A non existing event is not changing the existing consumptions"
    (is (= #::sim-de-rc{:currently-consuming
                        {#uuid "33497220-f844-11ee-9fa1-17acea14e9df"
                         #::sim-de-rc{:event {:a :b}
                                      :consumed-quantity 3}}}
           (sut/free #::sim-de-rc{:currently-consuming
                                  {#uuid "33497220-f844-11ee-9fa1-17acea14e9df"
                                   #::sim-de-rc{:event {:a :b}
                                                :consumed-quantity 3}}}
                     #uuid "33497220-f844-11ee-9fa1-17acea14e8ee"))))
  (testing "Empty currently consuming is ok"
    (is (= {::sim-de-rc/currently-consuming {}}
           (sut/free {::sim-de-rc/currently-consuming nil}
                     #uuid "33497220-f844-11ee-9fa1-17acea14e9df")))
    (is (= {::sim-de-rc/currently-consuming {}}
           (sut/free nil #uuid "33497220-f844-11ee-9fa1-17acea14e9df")))))
