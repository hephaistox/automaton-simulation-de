(ns automaton-simulation-de.rc.impl.unblocking-policy.registry
  "Registry for `unblocking-policy`."
  (:require
   [automaton-simulation-de.rc                             :as-alias sim-rc]
   [automaton-simulation-de.rc.impl.unblocking-policy.base
    :as sim-de-rc-unblocking-policy-base]
   [automaton-simulation-de.rc.unblocking-policy
    :as sim-de-rc-unblocking-policy]))

(defn schema
  "Schema of an `unblocking-policy` registry."
  []
  [:map-of :keyword (sim-de-rc-unblocking-policy/schema)])

(defn registry
  "The base policies `registry`."
  []
  #:automaton-simulation-de.rc{:FIFO
                               sim-de-rc-unblocking-policy-base/fifo-policy
                               :LIFO
                               sim-de-rc-unblocking-policy-base/lifo-policy})
