(ns automaton-simulation-de.scheduler.sorted-set-test
  (:require
   [automaton-simulation-de.scheduler.sorted-set :as sut]
   [automaton-simulation-de.scheduler :as simulation-scheduler]
   [automaton-simulation-de.events.quiet :as simulation-quiet]
   #?(:clj [clojure.test :refer [deftest is testing]]
      :cljs [cljs.test :refer [deftest is testing] :include-macros true])))

(defn- comparator-stub
  [a b]
  (<= (hash a)
      (hash b)))

(defn- keyword-sorter
  [a b]
  (apply compare
         (map (comp name :data) [a b])))

(deftest DiscreteEventScheduler-test
  (testing "Empty scheduler is returning nil?"
    (let [empty-scheduler (sut/make-discrete-event-scheduler comparator-stub)]
      (is (nil? (simulation-scheduler/next-event empty-scheduler)))
      (is (nil? (simulation-scheduler/last-event empty-scheduler)))
      (is (= empty-scheduler
             (simulation-scheduler/pop-event empty-scheduler)))
      (is (= #{}
             (simulation-scheduler/future-events empty-scheduler)))
      (is (simulation-scheduler/ended? empty-scheduler))
      (is (= []
             (simulation-scheduler/past-events empty-scheduler)))
      (is (= []
             (simulation-scheduler/all-events empty-scheduler)))))
  (testing "One element in the future"
    (let [one-evt-scheduler (-> (sut/make-discrete-event-scheduler keyword-sorter)
                                (simulation-scheduler/add-event (simulation-quiet/make-quiet-event 1 :a)))]
      (is (= :a
             (-> (simulation-scheduler/next-event one-evt-scheduler)
                 :data)))
      (is (= [:a]
             (->> one-evt-scheduler
                  simulation-scheduler/pop-event
                  :past-events
                  (mapv :data))))))
  (testing "Two elements in the future"
    (let [two-evts-scheduler (-> (sut/make-discrete-event-scheduler keyword-sorter)
                                 (simulation-scheduler/add-event (simulation-quiet/make-quiet-event 2 :b))
                                 (simulation-scheduler/add-event (simulation-quiet/make-quiet-event 1 :a)))]
      (is (= :a
             (-> (simulation-scheduler/next-event two-evts-scheduler)
                 :data)))
      (is (= [:a]
             (->> two-evts-scheduler
                  simulation-scheduler/pop-event
                  :past-events
                  (mapv :data))))
      (is (= [:b]
             (->> two-evts-scheduler
                  simulation-scheduler/pop-event
                  :future-events
                  (mapv :data))))
      (is (empty? (->> two-evts-scheduler
                       simulation-scheduler/pop-event
                       simulation-scheduler/pop-event
                       :future-events
                       (mapv :data))))))
  (testing "Date equality"
    (let [evts-scheduler (reduce
                          (fn [scheduler evt-to-add]
                            (->> (simulation-quiet/make-quiet-event 1 evt-to-add)
                                 (simulation-scheduler/add-event scheduler)))
                          (sut/make-discrete-event-scheduler keyword-sorter)
                          [:f :a :b :z :e :c :d :g])]
      (is (= [:a :b :c :d :e :f :g :z]
             (->> evts-scheduler
                 simulation-scheduler/future-events
                 (mapv :data)))))))

(deftest next-event-test
  (testing "Next event of an empty scheduler is returning nil"
    (is (nil? (-> (sut/make-discrete-event-scheduler comparator-stub)
                  simulation-scheduler/next-event)))
    (is (nil? (-> (sut/make-discrete-event-scheduler comparator-stub)
                  simulation-scheduler/pop-event
                  simulation-scheduler/next-event))))
  (testing "Next event of an empty scheduler is returning nil"))
