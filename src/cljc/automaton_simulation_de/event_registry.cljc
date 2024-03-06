(ns automaton-simulation-de.event-registry
  "Event registry aggregate details can be found in `docs/archi/event-registry-details.mermaid` and it's context in `docs/archi/event-registry-context.mermaid`"
  (:require
   [automaton-simulation-de.event-registry.event-ordering
    :as
    sim-de-event-registry-event-ordering]
   [automaton-simulation-de.event-registry.event-registry-kvs
    :as
    sim-de-event-registry-event-registry-kvs]))

(def event-registry-schema
  "Event registry gathers event registry kv collection and the ordering of events"
  [:map {:closed true}
   [:event-ordering sim-de-event-registry-event-ordering/event-ordering-schema]
   [:event-registry-kvs
    sim-de-event-registry-event-registry-kvs/event-registry-kvs-schema]])

(defn event-ordering-fn
  [event-registry]
  (->> event-registry
       :event-ordering
       (partial sim-de-event-registry-event-ordering/event-ordering)))

(defn event-registry-kvs [event-registry] (:event-registry-kvs event-registry))
