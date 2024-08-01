(ns automaton-simulation-de.transformation
  "Transformation contains useful functions for rendering
  * [See transformation big-picture](docs/archi/transformation/transformation_big_picture.png)"
  (:require
   [automaton-simulation-de.entity            :as-alias sim-entity]
   [automaton-simulation-de.rc                :as-alias sim-rc]
   [automaton-simulation-de.simulation-engine :as-alias sim-engine]))

(defn- get-path
  "Return value under path, if path is nil return value itself"
  [v path]
  (cond
    (keyword? path) (get v path)
    (sequential? path) (get-in v path)
    :else v))

(defn- keep-map
  "Filter out key/value pairs which values return false with `pred-fn`"
  ([pred-fn m] (keep-map pred-fn nil m))
  ([pred-fn path m]
   (into {} (filter (fn [[_ v]] (pred-fn (get-path v path))) m))))

;; Simulation engine
(defn keep-state
  "Keeps in simulation `state` only pairs that match with `pred-fn`"
  [pred-fn state]
  (keep-map pred-fn state))

(defn keep-events
  "Keep in coll of simulation `events` only those that match with `pred-fn`"
  [pred-fn events]
  (->> events
       (filter (fn [evt] (pred-fn evt)))))

(defn keep-snapshot-state
  "Filter out of `snapshot` state all items that do not match with `pred-fn` "
  ([keep-state-fn pred-fn snapshot]
   (-> snapshot
       (update ::sim-engine/state (partial keep-state-fn pred-fn))))
  ([pred-fn snapshot] (keep-snapshot-state keep-state pred-fn snapshot)))

(defn keep-snapshot-past-events
  "Filter out of `snapshot` past events all events that do not match with `pred-fn`"
  [pred-fn snapshot]
  (-> snapshot
      (update ::sim-engine/past-events (partial keep-events pred-fn))))

(defn keep-snapshot-future-events
  "Filter out of `snapshot` future events all events that do not match with `pred-fn`"
  [pred-fn snapshot]
  (-> snapshot
      (update ::sim-engine/future-events (partial keep-events pred-fn))))

(defn keep-snapshot-events
  "Filter out of `snapshot` ALL events that do not match with `pred-fn`"
  [pred-fn snapshot]
  (->> snapshot
       (keep-snapshot-past-events pred-fn)
       (keep-snapshot-future-events pred-fn)))

(defn keep-snapshot
  "Filter out of `snapshot` ALL events and state-items that do not match with `pred-fn`"
  [pred-fn snapshot]
  (->> snapshot
       (keep-snapshot-state pred-fn)
       (keep-snapshot-events pred-fn)))

(defn keep-snapshot-events-based-state
  "Filter out of `snapshot` ALL state-items that do not match with `pred-fn` and filter out all events that do not mention those state-items ids"
  ([pred-fn snapshot] (keep-snapshot-events-based-state pred-fn nil snapshot))
  ([pred-fn id snapshot]
   (keep-snapshot-events-based-state keys
                                     #(keep-snapshot-state pred-fn %)
                                     id
                                     snapshot))
  ([get-state-keys-fn keep-state-fn id snapshot]
   (let [snapshot (keep-state-fn snapshot)
         state-keys (get-state-keys-fn (::sim-engine/state snapshot))]
     (keep-snapshot-events #(some (fn [v] (= v (get % id))) state-keys)
                           snapshot))))

;; Simulation response
(defn keep-stopping-causes
  "Filter out all stopping-causes that do not match with `pred-fn`"
  [pred-fn stopping-causes]
  (filter (fn [stopping-cause] (pred-fn stopping-cause)) stopping-causes))

(defn keep-stopping-causes-by-model-end
  "Keep all `stopping-causes` that model end is matching `pred-fn`"
  [pred-fn stopping-causes]
  (filter (fn [stopping-cause]
            (pred-fn (get-in stopping-cause
                             [::sim-engine/stopping-criteria
                              ::sim-engine/model-end?])))
          stopping-causes))

(defn keep-stopping-causes-by-stopping-definition
  "Keep all `stopping-causes` that stopping definitio` is matching `pred-fn`"
  [pred-fn stopping-causes]
  (filter (fn [stopping-cause]
            (pred-fn (get-in stopping-cause
                             [::sim-engine/stopping-criteria
                              ::sim-engine/stopping-definition])))
          stopping-causes))

;; Multiple snapshots
(defn keep-snapshots-state
  "Expects sorted collection of `snapshots`, keeps only those that have difference between each other in filtered by `pred-fn` state"
  [pred-fn snapshots]
  (reduce (fn [acc snapshot]
            (let [last-snapshot-state (::sim-engine/state (last acc))]
              (if (= (::sim-engine/state snapshot) last-snapshot-state)
                acc
                (conj acc snapshot))))
          []
          (map #(keep-snapshot-state pred-fn %) snapshots)))

;; RC
(defn keep-state-resource
  "Keeps in simulation `state` only those resources that match with `pred-fn`"
  [pred-fn state]
  (update state ::sim-rc/resource (partial keep-state pred-fn)))

(defn keep-snapshot-state-resource
  "Keeps in simulation snapshot `state` only those resources that match with `pred-fn`"
  [pred-fn snapshot]
  (keep-snapshot-state keep-state-resource pred-fn snapshot))

(defn keep-snapshot-resource
  "Keeps in snapshot `state` only those resources that match with `pred-fn` and filters events with the same `pred-fn`"
  [pred-fn snapshot]
  (->> snapshot
       (keep-snapshot-state-resource pred-fn)
       (keep-snapshot-events pred-fn)))

(defn keep-snapshot-events-based-state-resource
  "Keeps in snapshot `state` only those resources that match with `pred-fn` and filters events that contains those resources ids"
  ([pred-fn snapshot]
   (keep-snapshot-events-based-state-resource pred-fn nil snapshot))
  ([pred-fn id snapshot]
   (keep-snapshot-events-based-state #(keys (get % ::sim-rc/resource))
                                     #(keep-snapshot-state-resource pred-fn %)
                                     id
                                     snapshot)))

;;Entity
(defn keep-state-entity
  "Keeps in simulation `state` only those entities that match with `pred-fn`"
  [pred-fn state]
  (update state
          ::sim-entity/entities
          (partial keep-map pred-fn ::sim-entity/entity-state)))

(defn keep-snapshot-state-entity
  "Keeps in simulation `snapshot` state only those entities that match with `pred-fn`"
  [pred-fn snapshot]
  (keep-snapshot-state keep-state-entity pred-fn snapshot))

(defn keep-snapshot-entity
  "Filter out of `snapshot` ALL events and state-entity-items that do not match with `pred-fn`"
  [pred-fn snapshot]
  (->> snapshot
       (keep-snapshot-state-entity pred-fn)
       (keep-snapshot-events pred-fn)))

(defn keep-snapshot-events-based-state-entity
  "Keeps in snapshot `state` only those entities that match with `pred-fn` and filters events that contains those resources ids"
  ([pred-fn snapshot]
   (keep-snapshot-events-based-state-entity pred-fn nil snapshot))
  ([pred-fn id snapshot]
   (keep-snapshot-events-based-state #(keys (::sim-entity/entities %))
                                     #(keep-snapshot-state-entity pred-fn %)
                                     id
                                     snapshot)))
