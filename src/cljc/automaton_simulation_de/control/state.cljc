(ns automaton-simulation-de.control.state
  "State containing information about and for control (control settings, current-iteration, computation-implementation to use...)
   For diagram see ![computation](archi/control/state.png)"
  (:refer-clojure :exclude [get set])
  (:require
   [automaton-core.adapters.schema                     :as core-schema]
   [automaton-simulation-de.control                    :as-alias sim-de-control]
   [automaton-simulation-de.simulation-engine.response :as sim-de-response]))

(defn rendering-state-schema
  "Schema of rendering state, `:computation` should be an object implementing computation defprotocol"
  []
  [:map {:closed true}
   [:pause? {:default true}
    :boolean]
   [:play-delay {:default 1000}
    number?]
   [:current-iteration {:optional true}
    sim-de-response/schema]
   [:computation :any]])

(defn build
  "Returns state in form of an atom, if `initial-state` is valid or nil otherwise"
  [initial-state]
  (let [schema (rendering-state-schema)
        initial-state (core-schema/add-default schema initial-state)]
    (when (core-schema/validate-data schema initial-state) (atom initial-state))))

(defn get [state] @state)

(defn set [state k v] (swap! state assoc k v))
