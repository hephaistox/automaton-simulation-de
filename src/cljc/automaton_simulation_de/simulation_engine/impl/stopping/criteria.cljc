(ns automaton-simulation-de.simulation-engine.impl.stopping.criteria
  "Declares a `stopping-criteria` to instantiate `stopping-definition`s, it precises the `params` necessary for `stopping-evaluation`.
  The data are:

  * `model-end?` is set to `true` if the stopping-criteria is one possible end of the model. Note that more than one is possible for the same model. Note that these model `stopping-criteria` are not supposed to be linked with anything else than the business model itself (not the rendering, not the control/computation, ...)
  * `params` is a map of parameters, which content is defined by the `stopping-definition`.
  * `stopping-definition` is the `stopping-definition` as found in the `stopping-registry`.

  ![entities][archi/simulation_engine/stopping_stopping-criteria.png]"
  (:require
   [automaton-core.adapters.schema                                     :as core-schema]
   [automaton-simulation-de.simulation-engine                          :as-alias sim-engine]
   [automaton-simulation-de.simulation-engine.impl.stopping.definition :as sim-de-sc-definition]))

(def schema
  [:map {:closed true}
   [::sim-engine/model-end? {:optional true}
    :boolean]
   [::sim-engine/params {:optional true}
    :map]
   [::sim-engine/stopping-definition sim-de-sc-definition/schema]])

(defn evaluates
  "Evaluates the `stopping-criteria` on `snapshot`.

  Returns `nil` if `stopping-evaluation` is not defined.
  Returns a map with `stop?` and `context`."
  [{::sim-engine/keys [params stopping-definition]
    :as stopping-criteria}
   snapshot]
  (let [{::sim-engine/keys [stopping-evaluation]} stopping-definition]
    (when (fn? stopping-evaluation)
      (let [{::sim-engine/keys [stop? context]} (stopping-evaluation snapshot params)]
        (when stop?
          {::sim-engine/context context
           ::sim-engine/stopping-criteria stopping-criteria})))))

(defn model-end
  "Set the `stopping-criteria` as one ending the model."
  [stopping-criteria]
  (assoc stopping-criteria ::sim-engine/model-end? true))

(defn out-of-model
  "Set the `stopping-criteria` as one not ending the model."
  [stopping-criteria]
  (assoc stopping-criteria ::sim-engine/model-end? false))

(defn api-data-to-entity
  "Turns the `api` version of a `stopping-criteria` to one matching the entity `schema`.

  Three forms are offered:

  * `keyword` for stopping-criteria that need no params.
  * `[stopping-definition-id params]` where `stopping-definition-id` is a keyword and `params` is a map.
  * or a criteria already turned to an entity

  All other forms are discarded."
  [stopping-registry stopping-criteria]
  (cond
    (keyword? stopping-criteria) (let [stopping-definition-id stopping-criteria]
                                   (when-let [stopping-definition (get stopping-registry
                                                                       stopping-definition-id)]
                                     {::sim-engine/params {}
                                      ::sim-engine/stopping-definition stopping-definition}))
    (vector? stopping-criteria)
    (let [[stopping-definition-id params] stopping-criteria]
      (when (and (or (nil? params) (map? params)) (keyword? stopping-definition-id))
        (when-let [stopping-definition (get stopping-registry stopping-definition-id)]
          {::sim-engine/params params
           ::sim-engine/stopping-definition stopping-definition})))
    (true? (core-schema/validate-data schema stopping-criteria)) stopping-criteria))
