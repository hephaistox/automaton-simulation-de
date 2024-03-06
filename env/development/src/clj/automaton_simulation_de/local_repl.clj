(ns automaton-simulation-de.local-repl
  "REPL entry point"
  (:require
   [automaton-core.repl :as core-repl])
  (:gen-class))

(defn -main "Main entry point for repl" [& _args] (core-repl/start-repl))
