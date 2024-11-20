(ns automaton-simulation-de.predicates.equality-predicates
  "Equality predicates compare values and return boolean based on the result"
  (:require
   [automaton-core.adapters.schema                :as core-schema]
   [automaton-simulation-de.predicates            :as-alias sim-pred]
   [automaton-simulation-de.predicates.impl.utils :as sim-trans-utils]))

(defn- in?
  "Return true if `value` is equal to one of values in `vs`, false otherwise"
  [value vs]
  (boolean (some (fn [v] (= v value)) vs)))

(defn- str-includes? [str text] (some? (re-find (re-pattern text) str)))

(defn- get-path
  "Return value under path, if path is nil return value itself"
  [path v]
  (cond
    (keyword? path) (get v path)
    (sequential? path) (get-in v path)
    (nil? path) v
    ;;:else added so cond don't throw error, but there is no real use-case for it except making this function total
    :else v))

(defn is?-fn
  "Returns predicate function that sa a param accepts value
  which will be compared with `v` and return true if it's equal, false if it's not.
  If `path` is supplied it will consider value under path for comparison"
  ([path v] #(= v (get-path path %)))
  ([v] (is?-fn nil v)))

(defn one-of?-fn
  "Returns predicate function that as a param accepts value which is compared with values in `vs`. If any of them are equal, predicate will return true.
   If `path` is supplied it will consider value under path for comparison"
  ([vs] (one-of?-fn nil vs))
  ([path vs] #(if (sequential? vs) (in? (get-path path %) vs) nil)))

(defn is-empty?-fn
  "Returns predicate fn that as a param accepts value which if nil or empty collection will return true.
   If `path` is supplied it will consider value under path for comparison"
  ([path]
   #(if-let [v (get-path path %)]
      (if (coll? v) (empty? v) false)
      true))
  ([] (is-empty?-fn nil)))

(defn true?-fn
  "Returns predicate fn that as a param accepts a value that if equals to true will return true.
   If `path` is supplied it will consider value under path for comparison"
  ([path] (is?-fn path true))
  ([] (is?-fn true)))

(defn false?-fn
  "Returns predicate fn that as a param accepts a value that if equals to false will return true.
   If `path` is supplied it will consider value under path for comparison"
  ([path] (is?-fn path false))
  ([] (is?-fn false)))

(defn contains?-fn
  "Returns pred fn that accepts any type data.
   Pred will return true if `value` exists in that data.
  If `path` is supplied it will consider value under path for comparison"
  ([path value]
   #(let [v (get-path path %)]
      (cond
        (string? v) (str-includes? v value)
        (map? v) (->> (sim-trans-utils/keypaths v)
                      (some (fn [path] ((is?-fn path v) value)))
                      boolean)
        (sequential? v) ((one-of?-fn v) value)
        :else ((is?-fn v) value))))
  ([text] (contains?-fn nil text)))

(defn starts-with?-fn
  "Returns pred fn that accepts string.
   Pred will return true if that string starts with `text`.
   If `path` is supplied it will consider value under path for comparison"
  ([path text]
   #(let [v (get-path path %)]
      (when (and (string? v) (string? text)) (str-includes? v (str "^" text)))))
  ([text] (starts-with?-fn nil text)))

(defn ends-with?-fn
  "Returns pred fn that accepts string.
   Pred will return true if that string ends with `text`.
   If `path` is supplied it will consider value under path for comparison"
  ([path text] #(let [v (get-path path %)] (when (string? v) (str-includes? v (str text "$")))))
  ([text] (ends-with?-fn nil text)))

(defn >-fn
  ([path v] #(let [ov (if (keyword? path) (get % path) (get-in % path))] (clojure.core/> ov v)))
  ([v] #(clojure.core/> % v)))

(defn >=-fn
  ([path v] #(let [ov (if (keyword? path) (get % path) (get-in % path))] (clojure.core/>= ov v)))
  ([v] #(clojure.core/>= % v)))

(defn <-fn
  ([path v] #(let [ov (if (keyword? path) (get % path) (get-in % path))] (clojure.core/< ov v)))
  ([v] #(clojure.core/< % v)))

(defn <=-fn
  ([path v] #(let [ov (if (keyword? path) (get % path) (get-in % path))] (clojure.core/<= ov v)))
  ([v] #(clojure.core/<= % v)))

(def pred-name :keyword)

(def path [:or :keyword :string [:vector [:or :keyword :string]]])

(defn- lang-schema
  "Utility function for creating schema for equality predicates"
  ([] (lang-schema true))
  ([strict?] (lang-schema strict? :any))
  ([strict? val-type]
   (if (not strict?) [:cat pred-name [:? path]] [:cat pred-name [:? path] val-type])))

(defn- lang-valid?
  "Returns true if predicate lang vector for equality predicate is valid"
  ([schema pred] (core-schema/validate-data schema pred))
  ([pred] (lang-valid? (lang-schema) pred)))

(def equality-predicates-lang-reg
  "Each predicate return boolean if comparison with pred-fn input can be done"
  {::sim-pred/always-true {:doc "Returns always true"
                           :pred-fn #(constantly true)}
   ::sim-pred/equal? {:doc "Compares if values are equal"
                      :pred-fn is?-fn
                      :validation-fn lang-valid?}
   ::sim-pred/one-of? {:doc "compares if one of values is equal to input"
                       :pred-fn one-of?-fn
                       :validation-fn (partial lang-valid?
                                               (conj (lang-schema false) [:sequential :any]))}
   ::sim-pred/is-empty? {:doc "compares if value nil or empty collection"
                         :pred-fn is-empty?-fn
                         :validation-fn (partial lang-valid? (lang-schema false))}
   ::sim-pred/contains? {:doc "Returns true if pred-fn input contains value, works for string"
                         :pred-fn contains?-fn
                         :validation-fn lang-valid?}
   ::sim-pred/starts-with? {:doc
                            "True if pred input starts with text, expects input to be a strings"
                            :pred-fn starts-with?-fn
                            :validation-fn (partial lang-valid? (lang-schema true :string))}
   ::sim-pred/ends-with? {:doc "True if pred input ends with text expects input to be a string"
                          :pred-fn ends-with?-fn
                          :validation-fn (partial lang-valid? (lang-schema true :string))}
   ::sim-pred/true? {:doc "Is true?"
                     :pred-fn true?-fn
                     :validation-fn (partial lang-valid? (lang-schema false))}
   ::sim-pred/false? {:doc "Is false?"
                      :pred-fn false?-fn
                      :validation-fn (partial lang-valid? (lang-schema false))}
   ::sim-pred/> {:doc "Is value greater than.."
                 :pred-fn >-fn
                 :validation-fn (partial lang-valid? (lang-schema true :number))}
   ::sim-pred/< {:doc "Is value lesser than.."
                 :pred-fn <-fn
                 :validation-fn (partial lang-valid? (lang-schema true :number))}
   ::sim-pred/>= {:doc "Is value greater or equal"
                  :pred-fn >=-fn
                  :validation-fn (partial lang-valid? (lang-schema true :number))}
   ::sim-pred/<= {:doc "Is value lesser or equal"
                  :pred-fn <=-fn
                  :validation-fn (partial lang-valid? (lang-schema true :number))}})
