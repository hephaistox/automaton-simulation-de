(ns automaton-simulation-de.rc.impl.preemption-policy.factory
  "Factory for `preemption-policy`."
  (:require
   [automaton-simulation-de.rc.impl.preemption-policy.base
    :as sim-de-rc-preemption-policy-base]))

(def default-policy sim-de-rc-preemption-policy-base/no-preemption)

(defn factory
  "`preemption-policy` factory."
  [registry preemption-policy]
  (get registry preemption-policy default-policy))
