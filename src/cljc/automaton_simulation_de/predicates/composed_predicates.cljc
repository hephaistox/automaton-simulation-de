(ns automaton-simulation-de.predicates.composed-predicates
  "Composed predicates accept other predicates and return value based on their value"
  (:require
   [automaton-core.adapters.schema     :as core-schema]
   [automaton-simulation-de.predicates :as-alias sim-pred]))

(defn not-fn
  "Returns true if `pred-fn` evaluates to false. Otherwise returns false."
  [pred-fn]
  #(not (pred-fn %)))

(defn and-fn
  "Returns true if ALL of `pred-fns` evaluate to true"
  [& pred-fn]
  #(every? (fn [pred-fn] (true? (pred-fn %))) pred-fn))

(defn or-fn
  "Returns true if ANY of `pred-fns` evaluates to true"
  [& pred-fn]
  #(boolean (some (fn [pred-fn] (true? (pred-fn %))) pred-fn)))

(def pred-name :keyword)

(defn- composed-pred-schema
  "Utility function for creating composed predicate schema"
  ([] (composed-pred-schema false))
  ([one-param?]
   [:schema {:registry
             {::composed-pred-schema
              [:cat
               pred-name
               (if one-param?
                 [:or [:cat :keyword [:* :any]] [:schema [:ref ::composed-pred-schema]]]
                 [:+ [:or [:cat :keyword [:* :any]] [:schema [:ref ::composed-pred-schema]]]])]}}
    [:ref ::composed-pred-schema]]))

(def composed-predicates-lang-reg
  "All predicates in composed predicates lang registry expects other predicates as params"
  {::sim-pred/not {:doc "Returns true if predicate evaluates to false, false otherwise"
                   :pred-fn not-fn
                   :validation-fn #(core-schema/validate-data (composed-pred-schema true) %)}
   ::sim-pred/and {:doc "Returns true if ALL of them evaluate to true"
                   :pred-fn and-fn
                   :validation-fn #(core-schema/validate-data (composed-pred-schema) %)}
   ::sim-pred/or {:doc "Returns true if ANY of them evaluates to true"
                  :pred-fn or-fn
                  :validation-fn #(core-schema/validate-data (composed-pred-schema) %)}})
