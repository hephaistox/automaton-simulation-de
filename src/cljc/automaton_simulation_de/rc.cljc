(ns automaton-simulation-de.rc
  "Models resource consumers interaction.

  Resource definition:
  * A limited quantity of items that are used (e.g. seized and disposed) by entities as they proceed through the system. A resource has a capacity that governs the total quantity of items that may be available. All the items in the resource are homogeneous, meaning that they are indistinguishable. If an entity attempts to seize a resource that does not have any units available it must wait in a queue. It is often representing real world items that availability is limited (e.g. machine, wrench).

  Consumer definition:
  * A consumer is responsible for seizing and disposing the resource.

  Note:
   * All namespaced keywords of the rc bounded context are from this namespace, so rc users need only to refer this one."
  (:require
   [automaton-simulation-de.rc.impl.preemption-policy.registry :as
                                                               sim-de-rc-preemption-policy-registry]
   [automaton-simulation-de.rc.impl.state                      :as sim-de-rc-state]
   [automaton-simulation-de.rc.impl.unblocking-policy.registry :as
                                                               sim-de-rc-unblocking-policy-registry]
   [automaton-simulation-de.simulation-engine                  :as-alias sim-engine]
   [automaton-simulation-de.simulation-engine.event-return     :as sim-de-event-return]))

(defn seize
  "Seize a resource,

  Depending on the capacity found in the state for that `resource-name`,
  * If capacity is not defined for that resource, the whole consumption is skipped, including the `postponed-event` execution.
  * If some resource are available, the `postponed-event` is executed now, (i.e. added at the current date in the scheduler)
  * Otherwise block that consumer and store it in the queue
     * Blocked consumer: A blocked consumer is a consumer waiting for a resource being available.
     * Queue: Stores blocked consumers.

  Returns an event-return."
  [{::sim-engine/keys [state]
    :as event-return}
   resource-name
   consumed-quantity
   seizing-date
   postponed-event]
  (let [[consumption-uuid state] (sim-de-rc-state/seize
                                  state
                                  resource-name
                                  consumed-quantity
                                  (when postponed-event
                                    (assoc postponed-event ::sim-engine/date seizing-date)))]
    (cond-> event-return
      consumption-uuid (sim-de-event-return/add-event
                        (assoc-in postponed-event [::resource resource-name] consumption-uuid)
                        seizing-date)
      (some? state) (assoc ::sim-engine/state state))))

(defn dispose
  "Returns the `event-return` with the resource disposed, so it is available again.

  A consumer is unblocked, the capacity of `resource-name` is freed."
  [event-return
   resource-name
   {::keys [resource]
    ::sim-engine/keys [date]
    :as _current-event}]
  (let [{::sim-engine/keys [state]} event-return
        [unblockings state]
        (sim-de-rc-state/dispose state resource-name (get resource resource-name))]
    (reduce (fn [event-return
                 {::keys [consumed-quantity seizing-event]
                  :as _blocking}]
              (seize event-return resource-name consumed-quantity date seizing-event))
            (assoc event-return ::sim-engine/state state)
            unblockings)))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn resource-update
  "Update the resource capacity."
  [{::sim-engine/keys [state]
    :as event-return}
   resource-name
   new-capacity]
  (let [[unblocked-events state]
        (sim-de-rc-state/update-resource-capacity state resource-name new-capacity)]
    (-> event-return
        (assoc ::sim-engine/state state)
        (sim-de-event-return/add-events unblocked-events))))

(defn unblocking-policy-registry
  "Returns the default registry for event `unblocking-policy`.
  Note that you can enrich it with your own policies."
  []
  (sim-de-rc-unblocking-policy-registry/registry))

(defn preemption-policy-registry
  "Returns the default registry for event `preemption-policy`.
  Note that you can enrich it with your own policies."
  []
  (sim-de-rc-preemption-policy-registry/registry))

(defn wrap-model
  "Wraps a model to add necessary behavior to model a resource/consumer.

  Resource/Consumer modeling is a way to model state and events for simulation, by using concepts of resource being used by consumer

  The `resources` is a map defining the resource available:
      * `policy` In a queue, the policy selects the next consumer that will be unblocked. (Each queue has its own policy)
      * `renewable?` When disposed, a renewable resource model is available again. Typically the toolings like wrenches, hammers, machines are most often renewable resources."
  [model
   {:keys [rc]
    :as _model-data}
   unblocking-policy-registry
   preemption-policy-registry]
  (cond-> model
    (seq rc) (update-in [::sim-engine/initial-snapshot ::sim-engine/state]
                        (fn [state]
                          (sim-de-rc-state/define-resources state
                                                            rc
                                                            unblocking-policy-registry
                                                            preemption-policy-registry)))))
