(ns automaton-simulation-de.rc.impl.preemption-policy.registry
  "Registry for `preemption-policy`."
  (:require
   [automaton-simulation-de.rc.impl.preemption-policy.base
    :as sim-de-rc-preemption-policy-base]
   [automaton-simulation-de.rc.preemption-policy
    :as sim-de-rc-preemption-policy]))

(defn schema
  "Schema for a `preemption-policy` registry."
  []
  [:map-of :keyword (sim-de-rc-preemption-policy/schema)])

(defn registry
  "The base policies for `preemption-policy`."
  []
  #:automaton-simulation-de-rc{:automaton-simulation-de.rc/no-preemption
                               sim-de-rc-preemption-policy-base/no-preemption})
