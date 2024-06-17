(ns automaton-simulation-de.rc.impl.resource.queue
  "The queue of a resource is containing all event execution which are blocked while waiting for a resource to be available."
  (:require
   [automaton-simulation-de.rc                      :as-alias sim-rc]
   [automaton-simulation-de.simulation-engine.event :as sim-de-event]))

(def schema
  "Schema of a `queue`.
  A queue should be a vector and not a general collection to preserve order."
  [:vector sim-de-event/schema])

(defn queue-event
  "Returns the `resource` with `sim-de-rc/queue` updated with a sequence containing maps:
  * `::sim-rc/consumed-quantity` is the `consumed-quantity`.
  * `::sim-rc/seizing-event` is the `event` .

  `consumed-quantity` should be a strictly positive integer, otherwise queueuing is ignored.

  This is called when the resource is not available and the event should be postponed for a moment later on when the resource will be available."
  [resource consumed-quantity event]
  (cond-> (or resource {})
    (and (integer? consumed-quantity) (pos-int? consumed-quantity) (seq event))
    (update ::sim-rc/queue
            (fnil #(conj %
                         #:automaton-simulation-de.rc{:seizing-event event
                                                      ::sim-rc/consumed-quantity
                                                      consumed-quantity})
                  []))))

(defn unqueue-event
  "Removes events in the queue of resource `resource`, based on policy chosen, as many as needed to fullfill the `available-capacity`, the capacity that is free now and that we would like to try to execute.

  Returns a two entry vector:
  * the `blockings` removed from the queue and which execution will be attempted.
  * and the updated resource without the event in its queue anymore.
  Note that it may happen that the availability changes before that event is really executed, (i.e. it may happen that capacity change, higher priority events change the number of available resources...)."
  [{::sim-rc/keys [queue cache]
    :as resource}
   available-capacity]
  (let [{::sim-rc/keys [unblocking-policy-fn]} cache]
    (cond
      (not (integer? available-capacity))
      (let [[_ new-queue] (unblocking-policy-fn queue)]
        [[]
         (assoc resource ::sim-rc/queue (if (empty? new-queue) [] new-queue))])
      (<= available-capacity 0) [[] resource]
      :else
      (loop [unblocked-events []
             queue queue
             released-capacity 0]
        (let [[blocking new-queue] (unblocking-policy-fn queue)
              blocked-quantity (get blocking ::sim-rc/consumed-quantity 1)
              new-released-quantity (+ released-capacity blocked-quantity)]
          (cond
            (nil? blocking)
            [unblocked-events
             (assoc resource ::sim-rc/queue (if (nil? new-queue) [] new-queue))]
            (>= available-capacity new-released-quantity)
            (recur (conj unblocked-events blocking)
                   new-queue
                   new-released-quantity)
            :else
            [unblocked-events
             (assoc resource ::sim-rc/queue (if (nil? queue) [] queue))]))))))
