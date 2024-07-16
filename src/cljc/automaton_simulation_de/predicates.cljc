(ns automaton-simulation-de.predicates
  "Predicates are functions that takes as a param a value and return true if the value matches predicate, false otherwise and nil if comparison can't be done.
   Predicate functions can be expressed via a query language represented with vector.
   This namespace is about managing that language"
  (:require
   [automaton-core.adapters.schema                         :as core-schema]
   [automaton-simulation-de.predicates.composed-predicates
    :as sim-trans-pred-composed]
   [automaton-simulation-de.predicates.equality-predicates
    :as sim-trans-pred-equality]
   [clojure.walk                                           :as walk]))

(def pred-lang-schema
  "Predicate vector language consists of :keyword represnting the name of predicate and zero or more parameters for that predicate
   [See predicate language](docs/archi/transformation/predicate_query.png)"
  [:cat :keyword [:* :any]])

(def registry-value
  [:map
   [:pred-fn fn?]
   [:validation-fn {:optional true}
    fn?]
   [:doc {:optional true}
    :string]])

(def pred-registry-schema
  "Predicate registry consists of keyword keys representing predicate name with map as a value, containing an implementation of the predicate under :pred-fn and metadata for the predicate"
  [:map-of :keyword registry-value])

(def predicates-registry
  "Default predicates language registry.
   [See more](docs/archi/transformation/predicate_registry.png)"
  (merge sim-trans-pred-equality/equality-predicates-lang-reg
         sim-trans-pred-composed/composed-predicates-lang-reg))

(defn is-predicate?
  [reg pred]
  (true? (and (core-schema/validate-data pred-lang-schema pred)
              (fn? (get-in reg [(first pred) :pred-fn])))))

(defn- predicate-valid?
  [reg pred]
  (let [validation-fn (get-in reg [(first pred) :validation-fn])]
    (if (fn? validation-fn) (true? (validation-fn pred)) true)))

(defn predicate-validation
  [reg pred]
  (try (clojure.walk/prewalk (fn [el]
                               (cond
                                 (and (is-predicate? reg el)
                                      (not (predicate-valid? reg pred)))
                                 (throw (ex-info "Predicate is not valid"
                                                 {:pred el
                                                  :reg reg}))
                                 :else el)
                               el)
                             pred)
       nil
       (catch #?(:clj Exception
                 :cljs :default)
         e
         {:error e}))
  nil)

(defn predicate-lang->predicate-fn
  "Translates `pred` vector language into a function. Expects `reg` map containing predicate name as keys with values containing a function under :pred-fn"
  [reg pred]
  (clojure.walk/postwalk
   (fn [el]
     (cond
       (keyword? el) (if-let [pred-fn (get-in reg [el :pred-fn])]
                       pred-fn
                       el)
       (and (vector? el) (fn? (first el))) (apply (first el) (rest el))
       :else el))
   pred))

(defn predicate-lang->pred-fn-detailed
  "Turns predicate query language into a function"
  ([pred] (predicate-lang->pred-fn-detailed predicates-registry pred))
  ([reg pred]
   (if-let [invalid-pred-schema
            (core-schema/validate-data-humanize pred-lang-schema pred)]
     {:msg "Predicate is not matching a schema"
      :error invalid-pred-schema}
     (if-let [invalid-pred-query (predicate-validation reg pred)]
       {:msg "Predicate is not valid"
        :error invalid-pred-query}
       (try (predicate-lang->predicate-fn reg pred)
            (catch #?(:clj Exception
                      :cljs :default)
              e
              {:msg "Error during transformation from language to function"
               :error e}))))))

(defn apply-query-detailed
  "Same as `apply-query`, but in case of an error it returns a map with more detailed information"
  ([pred d] (apply-query-detailed predicates-registry pred d))
  ([reg pred d]
   (let [pred-fn (predicate-lang->pred-fn-detailed reg pred)]
     (if (fn? pred-fn)
       (try (pred-fn d)
            (catch #?(:clj Exception
                      :cljs :default)
              e
              {:msg "predicate failed during execution"
               :reg reg
               :pred pred
               :data d
               :error e}))
       {:reg reg
        :msg "predicate does not evaluated to function"
        :pred pred
        :data d
        :error pred-fn}))))

(defn apply-query
  "Accepts `pred` query vector, translates it with `reg` and applies it to `d`
   Returns response from predicate or nil if application couldn't be done"
  ([pred d] (apply-query predicates-registry pred d))
  ([reg pred d]
   (try ((predicate-lang->predicate-fn reg pred) d)
        (catch #?(:clj Exception
                  :cljs :default)
          _
          nil))))
