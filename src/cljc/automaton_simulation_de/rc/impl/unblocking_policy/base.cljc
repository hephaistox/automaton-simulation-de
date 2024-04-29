(ns automaton-simulation-de.rc.impl.unblocking-policy.base
  "The simple policies to unblock an event in the queue.")

(defn fifo-policy
  "Select the first blocked consumer in the queue."
  [[next-unblocked-event & rest-blocked :as _queue]]
  [next-unblocked-event rest-blocked])

(defn lifo-policy
  "Select the last blocked consumer in the queue."
  [queue]
  [(last queue) (butlast queue)])
