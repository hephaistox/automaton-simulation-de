(ns automaton-simulation-de.rc.impl.state
  "Store and update resource consumer informations in the `:automaton-simulation-de.rc/resource` key of the state.
  Assuming state is associative."
  (:require
   [automaton-simulation-de.rc.impl.resource :as sim-de-rc-resource]))

(defn define-resources
  "Returns the state with the resources added in it, and values defaulted."
  [state
   defined-resources
   unblocking-policy-registry
   preemption-policy-registry]
  (update state
          :automaton-simulation-de.rc/resource
          (fn [resources]
            (merge resources
                   (into {}
                         (map (fn [[resource-name resource]]
                                [resource-name
                                 (assoc (sim-de-rc-resource/defaulting-values
                                         resource
                                         unblocking-policy-registry
                                         preemption-policy-registry)
                                        :automaton-simulation-de.rc/name
                                        resource-name)])
                              defined-resources))))))

(defn resource
  "Returns the resource called `resource-name`, nil if does not exist.
  The `resource-name` should be called as described in the `resources` map of the middleware"
  [state resource-name]
  (get-in state [:automaton-simulation-de.rc/resource resource-name]))

(defn- update-resource
  [state resource-name resource]
  (cond-> state
    (some? resource-name)
    (assoc-in [:automaton-simulation-de.rc/resource resource-name] resource)))

(defn update-resource-capacity
  "Returns a pair of:
  * `unblocked-events`
  * `state` where the resource called `resource-name` is set to its new capacity `new-capacity`."
  [state resource-name new-capacity]
  (let [resource (resource state resource-name)
        [unblocked-events resource]
        (sim-de-rc-resource/update-capacity resource new-capacity)]
    [unblocked-events (update-resource state resource-name resource)]))

(defn seize
  "Seize the resource called `resource-name` and update the `state` accordingly.
  `consuming-event` event that should be executed only when the `consumed-quantity` of `resource-name` was available for that event.

  Returns a pair:
  * `consumption-uuid` if the seizing found available resources.
  * the state, with the resource `resource-name` seizing `consumed-quantity` before the execution of the `posponed-event`."
  [state resource-name consumed-quantity consuming-event]
  (let [resource (resource state resource-name)]
    (if (or (nil? resource) (nil? consuming-event))
      [nil state]
      (let [[executed? resource] (sim-de-rc-resource/seize resource
                                                           consumed-quantity
                                                           consuming-event)]
        [executed? (update-resource state resource-name resource)]))))

(defn dispose
  "Returns a pair:
  * the `unblockings` event.
  * the `state`, with the resource `resource-name` consumption of `seizing-event` is disposed."
  [state resource-name consumption-uuid]
  (if (or (nil? consumption-uuid) (nil? resource-name))
    [[] state]
    (let [resource (resource state resource-name)
          [unblockings resource] (sim-de-rc-resource/dispose resource
                                                             consumption-uuid)]
      [unblockings (update-resource state resource-name resource)])))

(defn failure
  "Updates the `state` with the resource-name failing."
  [state _resource-name _seizing-event]
  (throw (ex-info "Not implemented yet" state)))
