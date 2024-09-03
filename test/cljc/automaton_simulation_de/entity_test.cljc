(ns automaton-simulation-de.entity-test
  (:require
   #?(:clj [clojure.test :refer [deftest is]]
      :cljs [cljs.test :refer [deftest is] :include-macros true])
   [automaton-core.adapters.schema                          :as core-schema]
   [automaton-simulation-de.entity                          :as sut]
   [automaton-simulation-de.simulation-engine               :as sim-engine]
   [automaton-simulation-de.simulation-engine.impl.model    :as sim-de-model]
   [automaton-simulation-de.simulation-engine.impl.registry :as sim-de-registry]))

(deftest create-test
  (is (= #::sut{:entities {:foo-entity #::sut{:created #::sut{:date 3}
                                              :living #::sut{:date 3}
                                              :entity-state {:foo :bar}}}}
         (sut/create {} 3 :foo-entity {:foo :bar}))
      "Adding a new entity that was not existing before.")
  (is
   (= #::sut{:entities {:foo-entity #::sut{:created #::sut{:date 4}
                                           :living #::sut{:date 5}
                                           :entity-state {:foo :bar
                                                          :a :b}
                                           :errors [#::sut{:why ::sut/already-created
                                                           :entity-name :foo-entity
                                                           :state
                                                           #::sut{:entities
                                                                  {:foo-entity
                                                                   #::sut{:created #::sut{:date 4}
                                                                          :entity-state {:a :b
                                                                                         :foo :c}}}}
                                                           :date 5
                                                           :entity-state {:foo :bar}}]}}}
      (sut/create #::sut{:entities {:foo-entity #::sut{:created #::sut{:date 4}
                                                       :entity-state {:a :b
                                                                      :foo :c}}}}
                  5
                  :foo-entity
                  {:foo :bar}))
   "Adding an already existing entity is merging the data map, but created lifecycle is not updated and an error is documented.
Note that `created` `date` is not modified on purpose as the real creation has happened before."))

(deftest errors-test
  (is (= {} (sut/errors {})) "No errors returns an empty map.")
  (is (= :list-of-errors
         (-> #::sut{:entities {:foo-entity #::sut{:created #::sut{:date 5}
                                                  :living #::sut{:date 5}
                                                  :entity-state {:foo :bar
                                                                 :a :b}
                                                  :errors :list-of-errors}
                               :ok-entity #::sut{}}}
             (sut/errors :foo-entity)))
      "Errors of a specific `entity-name` are returned.")
  (is (= {:foo-entity [:error1 :error2]}
         (-> #::sut{:entities {:foo-entity #::sut{:created #::sut{:date 5}
                                                  :living #::sut{:date 5}
                                                  :entity-state {:foo :bar
                                                                 :a :b}
                                                  :errors [:error1 :error2]}
                               :ok-entity #::sut{}}}
             sut/errors))
      "Errors are caught in the list, only entities where an error occur."))

(deftest error?-test
  (is (not (sut/error? {})) "An empty state is returning no error.")
  (is (sut/error? #::sut{:entities {:foo-entity #::sut{:created #::sut{:date 5}
                                                       :living #::sut{:date 5}
                                                       :entity-state {:foo :bar
                                                                      :a :b}
                                                       :errors :list-of-errors}
                                    :ok-entity #::sut{}}})
      "Detect one error."))

(def state-stub
  #::sut{:entities {:foo-entity #::sut{:created #::sut{:date 3}
                                       :living #::sut{:date 4}
                                       :entity-state {:data :of
                                                      :an :entity}}}})

(deftest update-test
  (is
   (= (-> state-stub
          (assoc-in [::sut/entities :foo-entity ::sut/entity-state :and] :another-data)
          (assoc-in [::sut/entities :foo-entity ::sut/living ::sut/date] 5))
      (-> state-stub
          (sut/update 5 :foo-entity assoc :and :another-data)))
   "When an existing entity that is living is updated, its entity state and living lifecycle are updated, the created is not modified.")
  (is
   (= (-> state-stub
          (update-in [::sut/entities :foo-entity ::sut/errors]
                     conj
                     #::sut{:why ::sut/exception-during-update
                            :entity-name :foo-entity
                            :state state-stub
                            :exception {}
                            :date 5}))
      (-> state-stub
          (sut/update 5 :foo-entity #(throw (ex-info "Hey no!" {:a %})))
          (update-in [::sut/entities :foo-entity ::sut/errors] vec)
          (assoc-in [::sut/entities :foo-entity ::sut/errors 0 ::sut/exception] {})))
   "If update raises an exception, the error is documented, the living date is not, as we consider the update did not happen.")
  (is
   (= [[#::sut{:why ::sut/updating-a-disposed-entity
               :state (-> state-stub
                          (assoc-in [::sut/entities :foo-entity ::sut/disposed] #::sut{:date 5}))
               :date 12
               :entity-name :foo-entity
               :function assoc
               :args [:bar :foo]}]
       {:data :of
        :an :entity
        :bar :foo}]
      ((juxt #(sut/errors % :foo-entity) #(sut/state % :foo-entity))
       (-> state-stub
           (assoc-in [::sut/entities :foo-entity ::sut/disposed] #::sut{:date 5})
           (sut/update 12 :foo-entity assoc :bar :foo))))
   "The update function documents an error if the entity is already disposed, the creation is marked in the lifecycle status.")
  (is (= [#::sut{:why ::sut/updating-a-not-created-entity
                 :state {}
                 :date 12
                 :entity-name :foo-entity
                 :function assoc
                 :args [:bar :foo]}]
         (-> {}
             (sut/update 12 :foo-entity assoc :bar :foo)
             (sut/errors :foo-entity)))
      "Update can update a non existing entity, the creation is marked in the lifecycle status."))

(deftest state-test
  (is (= {:foo :bar}
         (-> #::sut{:entities {:foo-entity #::sut{:created #::sut{:date 3}
                                                  :living #::sut{:date 3}
                                                  :entity-state {:foo :bar}}}}
             (sut/state :foo-entity)))
      "The state is returned.")
  (is (= nil (sut/state {} :non-existing-entity)) "No state returned for non existing."))

(deftest dispose-test
  (is (= #::sut{:entities {:foo-entity #::sut{:disposed #::sut{:date 10}
                                              :created #::sut{:date 3}
                                              :living #::sut{:date 5}}}}
         (-> #::sut{:entities {:foo-entity #::sut{:entity-state {:data :of
                                                                 :an :entity}
                                                  :created #::sut{:date 3}
                                                  :living #::sut{:date 5}}}}
             (sut/dispose 10 :foo-entity)))
      "Disposing an existing entity is updatng the lifecycle and remove is data.")
  (is (= #::sut{:entities {:foo-entity #::sut{:disposed #::sut{:date 10}
                                              :created #::sut{:date 10}
                                              :errors [#::sut{:why
                                                              ::sut/disposing-a-not-created-entity
                                                              :state {}
                                                              :date 10
                                                              :entity-name :foo-entity}]
                                              :living #::sut{:date 10}}}}
         (-> #::sut{}
             (sut/dispose 10 :foo-entity)))
      "Disposing a non existing entity creates it and its lifecycle data, and reports an error.")
  (is (= #::sut{:entities {:foo-entity
                           #::sut{:disposed #::sut{:date 10}
                                  :created #::sut{:date 3}
                                  :errors [#::sut{:why ::sut/already-disposed
                                                  :state #::sut{:entities
                                                                {:foo-entity
                                                                 #::sut{:disposed #::sut{:date 7}
                                                                        :created #::sut{:date 3}
                                                                        :living #::sut{:date 5}}}}
                                                  :date 10
                                                  :entity-name :foo-entity}]
                                  :living #::sut{:date 5}}}}
         (-> #::sut{:entities {:foo-entity #::sut{:disposed #::sut{:date 7}
                                                  :created #::sut{:date 3}
                                                  :living #::sut{:date 5}}}}
             (sut/dispose 10 :foo-entity)))
      "Disposing an already disposed entity reports an error."))

(deftest lifecycle-status-test
  (is (-> {}
          (sut/lifecycle-status :an-non-created-entity)
          empty?)
      "Non existing entity has no lifecycle status.")
  (is
   (= #::sut{:created #::sut{:date 3}
             :living #::sut{:date 11}}
      (-> #::sut{:entities {:foo-entity #::sut{:created #::sut{:date 3}
                                               :living #::sut{:date 11}
                                               :entity-state {:foo :bar}}}}
          (sut/lifecycle-status :foo-entity)))
   "When the entity is created, its lifecycle-status has its created and living fields at the same date.")
  (is (= #::sut{:created #::sut{:date 3}
                :living #::sut{:date 11}}
         (-> #::sut{:entities {:foo-entity #::sut{:created #::sut{:date 3}
                                                  :living #::sut{:date 11}
                                                  :entity-state {:foo :bar}}}}
             (sut/lifecycle-status :foo-entity)))
      "After update, the lifecycle has a new living date.")
  (is (= #::sut{:created #::sut{:date 3}
                :living #::sut{:date 11}
                :disposed #::sut{:date 15}}
         (-> #::sut{:entities {:foo-entity #::sut{:created #::sut{:date 3}
                                                  :living #::sut{:date 11}
                                                  :disposed #::sut{:date 15}
                                                  :entity-state {:foo :bar}}}}
             (sut/lifecycle-status :foo-entity)))
      "After disposed, the lifecycle has a disposed data."))

(deftest is-created?-test
  (is (not (some? (-> {}
                      (sut/is-created? :foo-entity))))
      "A non existing entity is not created.")
  (is (some? (-> #::sut{:entities {:foo-entity #::sut{:created #::sut{:date 3}
                                                      :living #::sut{:date 11}}}}
                 (sut/is-created? :foo-entity)))
      "A living entity is created.")
  (is (some? (-> #::sut{:entities {:foo-entity #::sut{:created #::sut{:date 3}
                                                      :living #::sut{:date 11}
                                                      :disposed #::sut{:date 15}}}}
                 (sut/is-created? :foo-entity)))
      "A disposed entity is created."))

(deftest is-living?-test
  (is (nil? (-> {}
                (sut/is-living? :foo-entity)))
      "A non existing entity is not living")
  (is (= #::sut{:date 11}
         (-> #::sut{:entities {:foo-entity #::sut{:created #::sut{:date 3}
                                                  :living #::sut{:date 11}}}}
             (sut/is-living? :foo-entity)))
      "A living entity is living")
  (is (nil? (-> #::sut{:entities {:foo-entity #::sut{:created #::sut{:date 3}
                                                     :living #::sut{:date 11}
                                                     :disposed #::sut{:date 15}}}}
                (sut/is-living? :foo-entity)))
      "A disposed entity is not living."))

(deftest is-disposed?-test
  (is (nil? (-> {}
                (sut/is-disposed? :foo-entity)))
      "A non existing entity is not disposed.")
  (is (nil? (-> #::sut{:entities {:foo-entity #::sut{:created #::sut{:date 3}
                                                     :living #::sut{:date 11}}}}
                (sut/is-disposed? :foo-entity)))
      "A living entity is not disposed.")
  (is (= #::sut{:date 15}
         (-> #::sut{:entities {:foo-entity #::sut{:created #::sut{:date 3}
                                                  :living #::sut{:date 11}
                                                  :disposed #::sut{:date 15}}}}
             (sut/is-disposed? :foo-entity)))
      "A disposed entity is disposed."))

(deftest wrap-model-test
  (is
   (=
    #::sim-engine{:registry
                  #::sim-engine{:stopping
                                #::sut{:entity-lifecycle-corrupted
                                       #::sim-engine{:doc
                                                     "Stops when an error occured in an entity lifecycle."
                                                     :id ::sut/entity-lifecycle-corrupted
                                                     :next-possible? true
                                                     :stopping-evaluation
                                                     sut/lifecycle-corrupted}}}}
    (->> {}
         sut/wrap-model))))

;;; Assembly tests
;;; ****************

(deftest assembly-model-test
  (is (= nil
         (->> (sim-de-model/build {::sim-engine/initial-event-type :IN} (sim-de-registry/build))
              sut/wrap-model
              (core-schema/validate-data-humanize sim-de-model/schema)))))

(def state0 {})

(def state1
  (-> state0
      (sut/create 3
                  :my-first-entity
                  {:data :of
                   :an :entity})))

(def state2
  (-> state1
      (sut/update 5 :my-first-entity assoc :and :another-data)))

(def state3
  (-> state2
      (sut/dispose 12 :my-first-entity)))

(deftest assembly-tests
  (is (nil? (sut/is-created? :non-existing state1)))
  (is (nil? (sut/is-living? :non-existing state1)))
  (is (nil? (sut/is-disposed? :non-existing state1)))
  (is (= #::sut{:date 3} (sut/is-created? state1 :my-first-entity)))
  (is (= #::sut{:date 3} (sut/is-living? state1 :my-first-entity)))
  (is (nil? (sut/is-disposed? state1 :my-first-entity)))
  (is (= {:data :of
          :an :entity
          :and :another-data}
         (sut/state state2 :my-first-entity)))
  (is (= #::sut{:date 5} (sut/is-living? state2 :my-first-entity)))
  (is (= {:data :of
          :an :entity
          :and :another-data}
         (-> state2
             (sut/state :my-first-entity))))
  (is (= nil
         (-> state3
             (sut/state :my-first-entity))))
  (is (= nil
         (-> state3
             (sut/is-living? :my-first-entity))))
  (is (= #::sut{:date 12}
         (-> state3
             (sut/is-disposed? :my-first-entity)))))
