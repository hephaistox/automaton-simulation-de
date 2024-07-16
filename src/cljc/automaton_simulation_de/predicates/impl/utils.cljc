(ns automaton-simulation-de.predicates.impl.utils
  (:require
   [clojure.walk :as walk]))

(defn remove-nils
  "remove pairs of key-value that has nil value from a (possibly nested) map. also transform map to nil if all of its value are nil"
  [nm]
  (walk/postwalk (fn [el]
                   (cond
                     (map? el) (not-empty
                                (into {} (remove (comp nil? second)) el))
                     (vector? el) (into [] (remove nil? el))
                     (list? el) (remove nil? el)
                     :else el))
                 nm))

(defn nilify-vals
  "It nilify whole keyword value if all values in map are nil"
  ([m] (nilify-vals [] m {}))
  ([base-path m result]
   (reduce-kv (fn [acc k v]
                (if (and (map? v) (not (every? nil? (vals v))))
                  (nilify-vals (conj base-path k) v acc)
                  (assoc-in acc (conj base-path k) nil)))
              result
              m)))

(defn keypaths
  ([m] (keypaths [] m []))
  ([base-path m result]
   (reduce-kv (fn [acc k v]
                (cond
                  (map? v) (keypaths (conj base-path k) v acc)
                  :else (conj acc (conj base-path k))))
              result
              m)))
