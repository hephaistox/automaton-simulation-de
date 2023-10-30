(ns automaton-simulation-de.repl-core
  "repl for `automaton-simulation-de` stand alone"
  (:require [automaton-core.log :as log]
            [automaton-core.repl :as repl]
            [clojure.core.async :refer [<! chan go]]
            [mount.core :as mount]
            [mount.tools.graph :as graph])
  (:gen-class))

(defn -main
  "Main entry point for repl"
  [& _args]
  (log/info "Start `automaton-simulation-de`")
  (log/trace "Component dependencies: " (graph/states-with-deps))
  (mount/start)
  (repl/start-repl)
  (let [c (chan 1)] (go (<! c))))
