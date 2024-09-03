(ns automaton-simulation-de.control-test
  (:require
   #?(:clj [clojure.test :refer [deftest is testing]]
      :cljs [cljs.test :refer [deftest is testing] :include-macros true])
   [automaton-simulation-de.control           :as sut]
   [automaton-simulation-de.control.state     :as sim-de-rendering-state]
   [automaton-simulation-de.demo.control      :as sim-demo-control]
   [automaton-simulation-de.simulation-engine :as-alias sim-engine]))

(defn- state-it-nb
  [state]
  (get-in (sim-de-rendering-state/get state)
          [:current-iteration ::sim-engine/snapshot ::sim-engine/iteration]))

(defn- snapshot-it-nb
  [control-response]
  (get-in control-response [::sut/response ::sim-engine/snapshot ::sim-engine/iteration]))

(deftest build-rendering-state-test
  (testing "Basic cases to build state"
    (is (some? (sut/build-rendering-state {:computation (sut/make-computation {} :direct)})))
    (is (number? (:play-delay @(sut/build-rendering-state {:computation
                                                           (sut/make-computation {} :direct)})))))
  (testing "Nil returned when initial-datat is incorrect"
    (is (nil? (sut/build-rendering-state {})))
    (is (nil? (sut/build-rendering-state nil)))
    (is (nil? (sut/build-rendering-state [])))
    (is (nil? (sut/build-rendering-state {:to-much-usless-data "whenever"
                                          :computation (sut/make-computation {} :direct)})))))

(defn make-direct-computation [model & args] (apply sut/make-computation model :direct args))

(defn make-chunk-computation [model & args] (apply sut/make-computation model :chunk args))

(defn- create-state
  []
  (sut/build-rendering-state {:computation (make-direct-computation (sim-demo-control/model))}))



(defn- create-endless-sim-state
  []
  (sut/build-rendering-state {:computation
                              (make-direct-computation (sim-demo-control/model-infinite) 1000)}))

(defn- create-early-stop-state
  []
  (sut/build-rendering-state {:computation (make-direct-computation
                                            (sim-demo-control/model-early-end))}))

(deftest move-x!-test
  (let [state (create-state)]
    (testing "Moving 1 iteration forward"
      (sut/move-x! state 1)
      (is (= 1 (state-it-nb state)))
      (sut/move-x! state 1)
      (is (= 2 (state-it-nb state)))
      (sut/move-x! state 1)
      (sut/move-x! state 1)
      (sut/move-x! state 1)
      (sut/move-x! state 1)
      (is (= 6 (state-it-nb state)))
      (is (= (state-it-nb state) (snapshot-it-nb (sut/move-x! (create-state) 6)))))
    (testing "Moving 1 iteration backward"
      (sut/move-x! state -1)
      (is (= 5 (state-it-nb state)))
      (sut/move-x! state -1)
      (is (= 4 (state-it-nb state)))
      (sut/move-x! state -1)
      (sut/move-x! state -1)
      (sut/move-x! state -1)
      (is (= 1 (state-it-nb state)))
      (let [new-state (create-state)
            _set-state-to-6-it (sut/move-x! new-state 5)]
        (is (= (state-it-nb state) (snapshot-it-nb (sut/move-x! new-state -5))))))
    (testing "Moving multiple iterations"
      (let [new-state (create-state)
            random-move (rand-int 10)]
        (sut/move-x! new-state 5)
        (is (= 5 (state-it-nb new-state)))
        (sut/move-x! new-state 5)
        (is (= 10 (state-it-nb new-state)))
        (sut/move-x! new-state random-move)
        (is (= (+ 10 random-move) (state-it-nb new-state)))
        (sut/move-x! new-state -8)
        (is (= (- (+ 10 random-move) 8) (state-it-nb new-state)))))
    (testing "edge cases"
      (is (nil? (sut/move-x! nil 5)))
      (is (nil? (sut/move-x! (atom {}) 5)))
      (sut/rewind! state)
      (sut/move-x! state 30)
      (sut/move-x! state 1)
      (sut/move-x! state 1)
      (sut/move-x! state 1)
      (sut/move-x! state 1)
      (is (= 31 (state-it-nb state)))
      (is (= 1 (snapshot-it-nb (sut/move-x! state -50))))
      (is (= 6 (snapshot-it-nb (sut/move-x! state 5)))))))

(deftest pause!-test
  (testing "Basic usage"
    (let [state (create-state)]
      (sut/pause! state)
      (is (= false (:pause? (sim-de-rendering-state/get state))))
      (sut/pause! state)
      (is (= true (:pause? (sim-de-rendering-state/get state))))
      (sut/pause! state true)
      (is (= true (:pause? (sim-de-rendering-state/get state))))
      (sut/pause! state false)
      (is (= false (:pause? (sim-de-rendering-state/get state))))
      (sut/pause! state false)
      (is (= false (:pause? (sim-de-rendering-state/get state)))))))

(deftest fast-foward?-test
  (testing "Basic usage"
    (let [state (create-state)] (is (= true (sut/fast-forward? state))))
    (let [state (create-endless-sim-state)] (is (= nil (sut/fast-forward? state))))))

(deftest fast-forward!-test
  (testing "If it didn't reach model-end but there is no next-possible it returns last iteration"
    (let [state (create-state)] (is (= 31 (snapshot-it-nb (sut/fast-forward! state)))))
    (testing
      "Here is a question, should it work when there is no model-end? Because in case of truly endless one we will need to secure API to not run endlessly (maybe some timeout?) and in case of simulation that has an end and no model-end, does it make sense to do it?"
      (let [state (create-endless-sim-state)]
        (is (= 1000 (snapshot-it-nb (sut/fast-forward! state))))))
    (testing "If there is a stop in simulation that's not model-end? it should continue"
      (let [state (create-early-stop-state)]
        (sut/move-x! state 10)
        (sut/rewind! state)
        (is (= 20 (snapshot-it-nb (sut/fast-forward! state))))))))

(deftest rewind!-test
  (testing "Rewinds to 1 iteration from any current point"
    (let [state (create-state)]
      (is (= 1 (snapshot-it-nb (sut/rewind! state))))
      (sut/fast-forward! state)
      (is (= 1 (snapshot-it-nb (sut/rewind! state))))
      (sut/move-x! state 1)
      (is (= 1 (snapshot-it-nb (sut/rewind! state))))
      (sut/move-x! state 15)
      (is (= 1 (snapshot-it-nb (sut/rewind! state))))
      (sut/rewind! state)
      (sut/rewind! state)
      (sut/rewind! state)
      (is (= 1 (snapshot-it-nb (sut/rewind! state)))))))

(deftest computation-change
  (let [state (create-state)]
    (sim-de-rendering-state/set state :computation nil)
    (sut/move-x! state 1)
    (is (= nil (state-it-nb state)))
    (sut/move-x! state 1)
    (is (= nil (state-it-nb state)))
    (sim-de-rendering-state/set state
                                :computation
                                (make-direct-computation (sim-demo-control/model)))
    (sut/move-x! state 1)
    (is (= 1 (state-it-nb state)))
    (sut/move-x! state 3)
    (is (= 4 (state-it-nb state)))
    (sim-de-rendering-state/set state :computation nil)
    (is (= 4 (state-it-nb state)))
    (sut/move-x! state 3)
    (is (= 4 (state-it-nb state)))
    (sim-de-rendering-state/set state
                                :computation
                                (make-direct-computation (sim-demo-control/model)))
    (sim-de-rendering-state/set state
                                :computation
                                (make-chunk-computation (sim-demo-control/model) 5))
    (is (= 4 (state-it-nb state)))
    (sut/move-x! state 1)
    (is (= 5 (state-it-nb state)))
    (sim-de-rendering-state/set state :computation nil)
    (sut/rewind! state)
    (is (= 5 (state-it-nb state)))
    (sut/fast-forward! state)
    (is (= 5 (state-it-nb state)))
    (sim-de-rendering-state/set state
                                :computation
                                (make-chunk-computation (sim-demo-control/model) 5))
    (sut/rewind! state)
    (is (= 1 (state-it-nb state)))
    (sut/fast-forward! state)
    (is (= 31 (state-it-nb state)))))
