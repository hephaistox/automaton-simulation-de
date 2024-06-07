(ns automaton-simulation-de.control.computation-test
  (:require
   #?(:clj [clojure.test :refer [deftest is testing]]
      :cljs [cljs.test :refer [deftest is testing] :include-macros true])
   [automaton-core.adapters.schema                       :as core-schema]
   [automaton-simulation-de.control                      :as sim-de-control]
   [automaton-simulation-de.control.computation          :as sut]
   [automaton-simulation-de.control.computation.response :as
                                                         sim-de-comp-response]
   [automaton-simulation-de.demo.control                 :as sim-demo-control]))

(defn- it-nb
  [resp]
  (get-in resp
          [:automaton-simulation-de.control/response
           :automaton-simulation-de.response/snapshot
           :automaton-simulation-de.scheduler.snapshot/iteration]))

(defn- state-stp-context
  [resp]
  (-> resp
      (get-in [:automaton-simulation-de.control/response
               :automaton-simulation-de.response/snapshot
               :automaton-simulation-de.scheduler.snapshot/state
               :m1
               :process])))

(defn model-regular [] (sim-demo-control/model))

(defn model-early-stop [] (sim-demo-control/model-early-end))

(defn model-infinite [] (sim-demo-control/model-infinite))

(defn create-direct-computation
  [model & args]
  (apply sim-de-control/make-computation model :direct args))

(deftest scheduler-response-test
  (let [regular-model (create-direct-computation (model-regular))
        infinite-model (create-direct-computation (model-infinite) 1000)
        model-with-end (create-direct-computation (model-early-stop))]
    (testing "Regular use-cases"
      (let [resp (sut/scheduler-response regular-model
                                         [[:iteration-nth {:n 10}]]
                                         0)]
        (is (nil? (core-schema/validate-data-humanize
                   sim-de-comp-response/schema
                   resp)))
        (is (= 10
               (-> resp
                   it-nb)))
        (is (= :success (:automaton-simulation-de.control/status resp))))
      (let [resp (sut/scheduler-response regular-model
                                         [[:iteration-nth {:n 10}]])]
        (is (nil? (core-schema/validate-data-humanize
                   sim-de-comp-response/schema
                   resp)))
        (is (= 10
               (-> resp
                   it-nb)))
        (is (= :success (:automaton-simulation-de.control/status resp))))
      (let [resp (sut/scheduler-response infinite-model
                                         [[:iteration-nth {:n 10}]]
                                         0)]
        (is (nil? (core-schema/validate-data-humanize
                   sim-de-comp-response/schema
                   resp)))
        (is (= 10
               (-> resp
                   it-nb)))
        (is (= :success (:automaton-simulation-de.control/status resp))))
      (let [resp (sut/scheduler-response model-with-end
                                         [[:iteration-nth {:n 15}]]
                                         0)]
        (is (= 15
               (-> resp
                   it-nb)))
        (is (= :success (:automaton-simulation-de.control/status resp))))
      (let [resp (sut/scheduler-response regular-model
                                         [[:iteration-nth {:n 50}]]
                                         0)]
        (is (= 32
               (-> resp
                   it-nb)))
        (is (= :no-next
               (-> resp
                   :automaton-simulation-de.control/status))))
      (let [resp (sut/scheduler-response model-with-end
                                         [[:iteration-nth {:n 50}]]
                                         0)]
        (is (= 20
               (-> resp
                   it-nb)))
        (is (= :no-next
               (-> resp
                   :automaton-simulation-de.control/status))))
      (let [resp (sut/scheduler-response infinite-model [] 0)]
        (is (= 1000
               (-> resp
                   it-nb)))
        (is (= :timeout
               (-> resp
                   :automaton-simulation-de.control/status))))
      (let [resp (sut/scheduler-response regular-model [] 0)]
        (is (nil? (core-schema/validate-data-humanize
                   sim-de-comp-response/schema
                   resp)))
        (is (= 32
               (-> resp
                   it-nb)))
        (is (= :success
               (-> resp
                   :automaton-simulation-de.control/status))))
      (let [resp (sut/scheduler-response regular-model)]
        (is (nil? (core-schema/validate-data-humanize
                   sim-de-comp-response/schema
                   resp)))
        (is (= 32
               (-> resp
                   it-nb)))
        (is (= :success
               (-> resp
                   :automaton-simulation-de.control/status))))
      (let [resp (sut/scheduler-response regular-model
                                         [[:state-contains {:state [:m1
                                                                    :process]}]]
                                         0)]
        (is (= 6
               (-> resp
                   it-nb)))
        (is (= :success
               (-> resp
                   :automaton-simulation-de.control/status)))
        (is (= 6
               (-> (sut/scheduler-response regular-model
                                           [[:state-contains {:state
                                                              [:m1 :process]}]]
                                           5)
                   it-nb)))))
    (testing "Manipulating iteration param is returning proper response"
      (let [mdw [[:state-contains {:state [:m1 :process]}]]]
        (is (= :p1
               (-> (sut/scheduler-response regular-model mdw 0)
                   state-stp-context))
            "Going from 0 should find specified stopping-criteria")
        (is (= :p1
               (-> (sut/scheduler-response regular-model mdw nil)
                   state-stp-context))
            "Nil is acceptable value for iteration number")
        (is (nil? (core-schema/validate-data-humanize
                   sim-de-comp-response/schema
                   (sut/scheduler-response regular-model mdw nil))))
        (is (= :p2
               (-> (sut/scheduler-response regular-model mdw 8)
                   state-stp-context))
            "Specifing further iteration should yield different result")
        (is (= :p3
               (-> (sut/scheduler-response regular-model mdw 9)
                   state-stp-context))
            "Specifing further iteration should yield different result")
        (is (= :p1
               (-> (sut/scheduler-response infinite-model mdw 15)
                   state-stp-context))
            "Specifing further iteration should yield different result")
        (is
         (let [resp (sut/scheduler-response regular-model mdw 20)]
           (and (= 32 (it-nb resp))
                (= :no-next (:automaton-simulation-de.control/status resp))))
         "Going outside of scope should return last possible iteration in finite simulation and fail status")))))
