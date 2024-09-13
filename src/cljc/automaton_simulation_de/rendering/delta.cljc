(ns automaton-simulation-de.rendering.delta
  "Uses editscript as this library is most performant, production used, considers both size optimization and speed
   https://github.com/juji-io/editscript"
  (:require
   [editscript.core :as editscript]))

(defn diff [m1 m2] (editscript/diff m1 m2))

(defn show-diff [diff] (editscript/get-edits diff))

(defn patch [m diff] (editscript/patch m diff))
