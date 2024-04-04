(ns automaton-simulation-de.local-repl
  "REPL entry point"
  (:require
   [automaton-core.repl :as core-repl])
  (:gen-class))

(defn -main
  "Main entry point for repl"
  [& args]
  (core-repl/start-repl args (core-repl/default-middleware) (constantly nil)))
