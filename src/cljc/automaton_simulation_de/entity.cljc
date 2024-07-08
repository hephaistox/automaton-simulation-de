(ns automaton-simulation-de.entity
  "An entity is a part of the model living in the `state` with its lifecycle managed.

  * ![entity](archi/entity/entity.png)
  * ![entity state](archi/entity/entity_state.png)
  * ![lifestatus](archi/entity/lifestatus.png)"
  (:refer-clojure :exclude [update])
  (:require
   [automaton-simulation-de.simulation-engine
    :as-alias sim-engine]
   [automaton-simulation-de.simulation-engine.impl.stopping-definition.registry
    :as sim-de-stopping-registry]))

(defn create
  "Creates an entity called `:entity-name` with `entity-data`. The lifecycle of this entity starts at `date`.

  An error is documented if the entity is created already."
  [state date entity-name entity-data]
  (-> state
      (update-in
       [::entities entity-name]
       (fn [entity]
         (let [created-already? (some? (get entity ::created))]
           (cond-> entity
             created-already? (clojure.core/update ::errors
                                                   concat
                                                   [#::{:why ::already-created
                                                        :entity-name entity-name
                                                        :state state
                                                        :entity-state
                                                        entity-data
                                                        :date date}])
             (not created-already?) (assoc ::created {::date date})
             :else (assoc ::living {::date date})
             :else (clojure.core/update ::entity-state merge entity-data)))))))

(defn errors
  "Returns entity errors if exists with a map associating a collection of errors to an `entity-name`"
  ([state]
   (->> (get state ::entities)
        (mapv (fn [[entity-name entity]]
                (when-let [errors (get entity ::errors)] [entity-name errors])))
        (filter (comp not empty? second))
        (into {})))
  ([state entity-name] (get-in state [::entities entity-name ::errors])))

(defn error?
  "Detects an eror in the entities."
  [{::keys [entities]
    :as _state}]
  (->> entities
       vals
       (filter ::errors)
       first
       some?))

(defn update
  "Update the entity called `:entity-name` with `f` applied to the existing value together with arguments `args`.
  The update of the entity is mark in the `living` lifecycle field."
  [state date entity-name f & args]
  (try
    (-> state
        (update-in
         [::entities entity-name]
         (fn [entity]
           (cond-> entity
             (some? (::disposed entity)) (clojure.core/update
                                          ::errors
                                          conj
                                          #::{:why ::updating-a-disposed-entity
                                              :state state
                                              :date date
                                              :entity-name entity-name
                                              :function f
                                              :args args})
             (nil? (::created entity)) (assoc ::created #::{:date date})
             (nil? (::created entity)) (clojure.core/update
                                        ::errors
                                        conj
                                        #::{:why ::updating-a-not-created-entity
                                            :state state
                                            :date date
                                            :entity-name entity-name
                                            :function f
                                            :args args})
             :else (assoc-in [::living ::date] date)
             :else
             (clojure.core/update ::entity-state (partial apply f) args)))))
    (catch #?(:clj Exception
              :cljs :default)
      e
      (-> state
          (update-in [::entities entity-name ::errors]
                     concat
                     [#::{:why ::exception-during-update
                          :entity-name entity-name
                          :state state
                          :date date
                          :exception e}])))))

(defn state
  "Returns the state value of the entity called `entity-name`."
  [state entity-name]
  (get-in state [::entities entity-name ::entity-state]))

(defn dispose
  "Disposing an entity by its `entity-name` is removing its data, its lifecycle will mark `::disposed` at the current `date`."
  [state date entity-name]
  (-> state
      (update-in
       [::entities entity-name]
       (fn [entity]
         (cond-> entity
           (nil? (::created entity)) (assoc-in [::created ::date] date)
           (nil? (::created entity)) (assoc-in [::living ::date] date)
           (some? (::disposed entity)) (clojure.core/update
                                        ::errors
                                        conj
                                        #::{:why ::already-disposed
                                            :state state
                                            :date date
                                            :entity-name entity-name})
           (nil? (::created entity)) (clojure.core/update
                                      ::errors
                                      conj
                                      #::{:why ::disposing-a-not-created-entity
                                          :state state
                                          :date date
                                          :entity-name entity-name})
           :else (assoc-in [::disposed ::date] date)
           :else (dissoc ::entity-state))))))

(defn lifecycle-status
  "The lifecycle has three possible fields `::created`, `::living` or `:disposed` depending on the position of the entity in its lifecycle."
  [state entity-name]
  (-> state
      (get-in [::entities entity-name])
      (select-keys [::created ::living ::disposed])))

(defn is-created?
  "Is the entity called `:entity-name` living?"
  [state entity-name]
  (let [{::keys [created]} (get-in state [::entities entity-name])] created))

(defn is-living?
  "Is the entity called `:entity-name` living?"
  [state entity-name]
  (let [{::keys [living disposed]} (get-in state [::entities entity-name])]
    (when (and living (not disposed)) living)))

(defn is-disposed?
  "Is the entity called `:entity-name` disposed?"
  [state entity-name]
  (let [{::keys [disposed]} (get-in state [::entities entity-name])] disposed))

(defn lifecycle-corrupted
  "Detects if one entity has a lifecycle error."
  [{::sim-engine/keys [state]
    :as _snapshot}
   _params]
  (when (error? state)
    {:stop? true
     :context (errors state)}))

(defn stopping-definition
  []
  #:automaton-simulation-de.simulation-engine{:doc
                                              "Stops when an error occured in an entity lifecycle."
                                              :id ::entity-lifecycle-corrupted
                                              :next-possible? true
                                              :stopping-evaluation
                                              lifecycle-corrupted})

(defn wrap-model
  "Wraps a model to add necessary behavior to model an entity."
  [model]
  (-> model
      (update-in [::sim-engine/registry ::sim-engine/stopping]
                 sim-de-stopping-registry/add-stopping-definition
                 (stopping-definition))))
