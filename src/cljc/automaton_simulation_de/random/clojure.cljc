(ns automaton-simulation-de.random.clojure
  "Basic random number generator"
  (:require [automaton-simulation-de.random :as simulation-random]))

(defrecord ClojureGenerator [seed float-precision]
  simulation-random/RandomNumberGenerator
    (next-int [_ max-int] (rand-int max-int))
    (next-float [_ min max]
      (/ (rand (* (- max min) float-precision)) float-precision)))

(defn make-clojure-generator
  ([seed float-precision] (->ClojureGenerator seed float-precision))
  ([]
   (->ClojureGenerator #?(:clj (System/currentTimeMillis)
                          :cljs (hash (js/Date.)))
                       1000.0)))
