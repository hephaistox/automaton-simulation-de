#_{:heph-ignore {:reports false}}
(ns automaton-simulation-de.rc.impl.resource
  "A resource is a limited quantity of items that are used by entities as they proceed through the system. A resource has a capacity that governs the total quantity of items that may be available. All the items in the resource are homogeneous, meaning that they are indistinguishable. If an entity attempts to seize a resource that does not have any units available it must wait in a queue.
  It is often representing real world items that availability is limited (e.g. machine, wrench, ...).

  A resource knows its instantenous capacity, its policies,and the element waiting for its availablility in the queue. The properties of a resource are:
  * `capacity` (default 1) total number of available resources, note that this number may evolve over time.
  * `currently-consuming` (default []) list of events currently consuming this resource, is useful to track them and enabling preemption and failures. The event contains the information on the waiting entity.
  * `preemption-policy` (default ::sim-de-rc/no-preemption) is the policy to apply when a resource is consumed and none is available.
  * `queue` (default []) list of blocked entities.
  * `renewable?` (default true) when true, the disposing is not giving back the values.
  * `unblocking-policy` (default ::simde-rc/FIFO) refers to an entity from the `automaton-simulation-de.rc.policies` registry, that registry will allow to pick one element in a queue."
  (:require
   [automaton-simulation-de.rc                                :as-alias sim-rc]
   [automaton-simulation-de.rc.impl.preemption-policy.factory
    :as sim-de-rc-preemption-policy-factory]
   [automaton-simulation-de.rc.impl.resource.consumption
    :as sim-de-rc-consumption]
   [automaton-simulation-de.rc.impl.resource.queue            :as
                                                              sim-de-rc-queue]
   [automaton-simulation-de.rc.impl.unblocking-policy.factory
    :as sim-de-rc-unblocking-policy-factory]))

(defn defaulting-values
  "Returns a resource with default values added."
  [{::sim-rc/keys [capacity
                   currently-consuming
                   preemption-policy
                   queue
                   renewable?
                   unblocking-policy]
    :as resource
    :or {capacity 1
         currently-consuming {}
         preemption-policy ::sim-rc/no-preemption
         queue []
         renewable? true
         unblocking-policy ::sim-rc/FIFO}}
   unblocking-policy-registry
   preemption-policy-registry]
  (assoc resource
         ::sim-rc/capacity capacity
         ::sim-rc/currently-consuming currently-consuming
         ::sim-rc/preemption-policy preemption-policy
         ::sim-rc/queue queue
         ::sim-rc/renewable? renewable?
         ::sim-rc/unblocking-policy unblocking-policy
         ::sim-rc/cache {::sim-rc/unblocking-policy-fn
                         (sim-de-rc-unblocking-policy-factory/factory
                          unblocking-policy-registry
                          unblocking-policy)
                         ::sim-rc/preemption-policy-fn
                         (sim-de-rc-preemption-policy-factory/factory
                          preemption-policy-registry
                          preemption-policy)}))

(defn nb-consumed-resources
  "Returns the number of consumed resources."
  [{:keys [::sim-rc/currently-consuming]
    :as _resource}]
  (if (map? currently-consuming)
    (->> currently-consuming
         vals
         (map (fn [{::sim-rc/keys [consumed-quantity]
                    :or {consumed-quantity 1}}]
                consumed-quantity))
         (apply + 0))
    0))

(defn nb-available-resources
  "Returns the number of available resources based on the defined `capacity,` and the `currently-consuming` resources (i.e. sum of their )."
  [{::sim-rc/keys [capacity]
    :or {capacity 1}
    :as resource}]
  (max 0 (- (or capacity 0) (nb-consumed-resources resource))))

(defn seize
  "Returns a pair:
  * the `consumption-uuid` if it has happened, nil if the execution is postponed awaiting for a resource disposal
  * the `state` reflecting the `resource` consumption

  The `seizing-event` is added in the `currently-seizing` so:
     * the number of available resources can be calculated, based on the used `quantity`
     * in case the seizing event should be cancelled (failures or preemption), the events are found in that list."
  [resource consumed-quantity postponed-event]
  (if (>= (compare (nb-available-resources resource) consumed-quantity) 0)
    (sim-de-rc-consumption/consume resource consumed-quantity postponed-event)
    [nil
     (sim-de-rc-queue/queue-event resource consumed-quantity postponed-event)]))

(defn dispose
  "Returns a pair of:
  * `unblockings` list of blockings that should be try to seize again the resource
  * `resource` updated with consumption `consumption-uuid` removed."
  [resource consumption-uuid]
  (let [resource (sim-de-rc-consumption/free resource consumption-uuid)]
    (sim-de-rc-queue/unqueue-event resource (nb-available-resources resource))))

(defn update-capacity
  "Returns a pair of:
  * `unblocked-events` list of events that should be try to seize again the
  * `resource` updated with the resource capacity

  If the new capacity is lower than the number of element consumed (i.e. in `currently-consuming`), then the `preemption-policy` choose one event to stop:
  * `::no-premption` is the only implemented, it doesn't do anything and let the currently executing event finish."
  [{::sim-rc/keys [cache capacity]
    :or {capacity 1}
    :as resource}
   new-capacity]
  (let [{::sim-rc/keys [preemption-policy-fn]} cache
        resource (assoc resource ::sim-rc/capacity new-capacity)]
    (if (< new-capacity capacity)
      (preemption-policy-fn resource)
      ;; Capacity increase
      (sim-de-rc-queue/unqueue-event resource
                                     (nb-available-resources resource)))))
