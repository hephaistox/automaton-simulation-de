(ns automaton-simulation-de.rc.impl.unblocking-policy.factory
  "Factory to return the `unblocking-policy`."
  (:require
   [automaton-simulation-de.rc.impl.unblocking-policy.base :as sim-de-rc-unblocking-policy-base]))

(def default-policy sim-de-rc-unblocking-policy-base/fifo-policy)

(defn factory
  "Policy factory, defaulting to `fifo-policy`."
  [registry policy]
  (get registry policy default-policy))
