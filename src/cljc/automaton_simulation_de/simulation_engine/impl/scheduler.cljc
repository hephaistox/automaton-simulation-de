(ns automaton-simulation-de.simulation-engine.impl.scheduler
  "The `scheduler` is working with a `model` describing the problem to solve.

  For each scheduler snapshot created, the scheduler is sorting the `future-events` based on the event ordering defined in the event-registry. The first event of that order is executed (see event execution). The resulting new values of state, past events, future events are used to create the new scheduler snapshot.

  An event has three parameters `(event-execution current-event state new-future-events)`:
  * `current-event` which is the current event to execute
  * `state` which is the state value before the event execution
  * `new-future-events` which is the list of future events without the current event

  The returned value is a `event-return`, which future events have no needs to be sorted, they will be by the scheduler.

  ![entities](archi/scheduler_entity.png)
  ![aggregate](archi/scheduler_aggregate.png)
  ![state diagram](archi/scheduler_state.png)"
  (:require
   [automaton-core.adapters.schema
    :as core-schema]
   [automaton-simulation-de.simulation-engine
    :as-alias sim-engine]
   [automaton-simulation-de.simulation-engine.event-return
    :as sim-de-event-return]
   [automaton-simulation-de.simulation-engine.impl.built-in-sd.execution-not-found
    :as sim-de-execution-not-found]
   [automaton-simulation-de.simulation-engine.impl.built-in-sd.failed-event-execution
    :as sim-failed-event-execution]
   [automaton-simulation-de.simulation-engine.impl.built-in-sd.no-future-events
    :as sim-de-no-future-events]
   [automaton-simulation-de.simulation-engine.impl.middleware.registry
    :as sim-de-middleware-registry]
   [automaton-simulation-de.simulation-engine.impl.middlewares
    :as sim-de-middlewares]
   [automaton-simulation-de.simulation-engine.impl.model
    :as sim-de-model]
   [automaton-simulation-de.simulation-engine.impl.model-data
    :as sim-de-model-data]
   [automaton-simulation-de.simulation-engine.impl.stopping.criteria
    :as sim-de-criteria]
   [automaton-simulation-de.simulation-engine.ordering
    :as sim-de-ordering]
   [automaton-simulation-de.simulation-engine.response
    :as sim-de-response]
   [automaton-simulation-de.simulation-engine.snapshot
    :as sim-de-snapshot]))

(defn handler
  "The handler execution is based on `request` data and is returning a `response`, it could be enriched with `middlewares`.

  The `response` is initiated  with the `stopping-causes` of the `request`, and the `snapshot`. If succesful, it is updated with `state` and `future-events` of the `event-execution`.

  It may happen that errors occur and `handler` creates `stopping-cause` as:

  * `sim-de-execution-not-found` if an `event-execution` is not found, that event is moved to `past-events` and the `stopping-cause` added.
  * `sim-failed-event-execution` if an exception is raised during `event-execution`, the `stopping-cause` is added."
  [{::sim-engine/keys
    [stopping-causes snapshot event-execution sorting current-event]
    :as _request}]
  (let [response #:automaton-simulation-de.simulation-engine{:stopping-causes
                                                             stopping-causes
                                                             :snapshot
                                                             snapshot}]
    (cond
      (seq stopping-causes) response
      (not (fn? event-execution))
      (-> response
          (sim-de-execution-not-found/evaluates
           (get-in snapshot [::sim-engine/future-events 0]))
          (sim-de-response/consume-first-event current-event))
      :else (try (let [{::sim-engine/keys [state future-events]} snapshot
                       [current-event & future-events-wo-current] future-events
                       event-return (event-execution current-event
                                                     state
                                                     future-events-wo-current)]
                   (-> response
                       (sim-de-response/consume-first-event current-event)
                       (update ::sim-engine/snapshot
                               sim-de-event-return/update-snapshot
                               event-return)
                       (update ::sim-engine/snapshot
                               sim-de-snapshot/sort-future-events
                               sorting)))
                 (catch #?(:clj Exception
                           :cljs :default)
                   e
                   (-> response
                       (sim-failed-event-execution/evaluates e current-event)
                       (sim-de-response/consume-first-event current-event)))))))

(defn scheduler-loop
  "`scheduler-loop` is one loop iteration of the scheduler.

  It is picking the first `event` of the `future-events` is the `current-event`, used to get the `event-execution`.
  It starts with the build of a `request`, call the `handler` wrapped in `middlewares` and add `current-event` to the potential `stopping-causes`

  Some error may create some `stopping-causes`, they are not stopping execution here, to give a chance to middlewares to add some other `stopping-causes`:

  * `no-future-events` which is happening when no event is found in the `future-events` to be executed."
  [event-registry
   sorting
   ahandler
   {::sim-engine/keys [future-events]
    :as snapshot}
   stopping-criterias]
  (let [current-event (first future-events)
        event-execution (get event-registry (::sim-engine/type current-event))
        stopping-causes (->> stopping-criterias
                             (mapv #(sim-de-criteria/evaluates % snapshot))
                             (filter some?))
        request #:automaton-simulation-de.simulation-engine{:stopping-causes
                                                            stopping-causes
                                                            :snapshot snapshot
                                                            :event-execution
                                                            event-execution
                                                            :current-event
                                                            current-event
                                                            :sorting sorting}]
    (-> request
        (sim-de-no-future-events/evaluates future-events)
        ahandler
        (sim-de-response/add-current-event-to-stopping-causes current-event))))

(defn invalid-inputs
  "Returns a map describing why it is invalid or `nil` if it is valid."
  [model scheduler-middlewares scheduler-stopping-criterias snapshot]
  (let
    [validate-data
     #:automaton-simulation-de.simulation-engine{:model
                                                 (core-schema/validate-data-humanize
                                                  sim-de-model/schema
                                                  model)
                                                 :snapshot
                                                 (core-schema/validate-data-humanize
                                                  sim-de-snapshot/schema
                                                  snapshot)
                                                 :scheduler-middlewares
                                                 (core-schema/validate-data-humanize
                                                  sim-de-model-data/middlewares-schema
                                                  scheduler-middlewares)
                                                 :scheduler-stopping-criteria
                                                 (core-schema/validate-data-humanize
                                                  sim-de-model-data/stopping-criterias-schema
                                                  scheduler-stopping-criterias)}]
    (when (not-every? nil? (vals validate-data)) validate-data)))

(defn scheduler
  "The scheduler executes the `model` until a `stopping-cause` is met.

  Note that users can enrich the execution of it by:
  * enriching the `model` with augmented registries.
  * supplementary middlewares and stopping criteria that don't affect the model

  Note that particular attention has been paid to leverage model's preparation, e.g. stopping-criteria and middlewares aren't translated again, just their `scheduler` version is.

  Returns a `response` with the last snapshot and the `stopping-causes`."
  [model scheduler-middlewares scheduler-stopping-criterias snapshot]
  (let [{::sim-engine/keys [registry middlewares ordering stopping-criterias]}
        model
        event-registry (::sim-engine/event registry)
        updated-middlewares (->> scheduler-middlewares
                                 (map (partial
                                       sim-de-middleware-registry/data-to-fn
                                       (::sim-engine/middleware registry)))
                                 (filterv some?))
        updated-scs (->> scheduler-stopping-criterias
                         (map (partial sim-de-criteria/api-data-to-entity
                                       (::sim-engine/stopping registry)))
                         (filter some?)
                         (mapv sim-de-criteria/out-of-model)
                         (concat stopping-criterias))
        initial-snapshot
        (if (map? snapshot)
          snapshot
          #:automaton-simulation-de.simulation-engine{:id 1
                                                      :iteration 1
                                                      :date nil
                                                      :state {}
                                                      :past-events []
                                                      :future-events []})
        sorting (sim-de-ordering/sorter ordering)
        wrapped-handler (->> updated-middlewares
                             (sim-de-middlewares/concat-supp-middlewares
                              middlewares)
                             (sim-de-middlewares/wrap-handler handler))
        sorted-snapshot
        (update initial-snapshot ::sim-engine/future-events sorting)]
    (loop [iteration-nb 1
           snapshot sorted-snapshot]
      (let [response (scheduler-loop event-registry
                                     sorting
                                     wrapped-handler
                                     snapshot
                                     updated-scs)
            {stopping-causes ::sim-engine/stopping-causes
             result-snapshot ::sim-engine/snapshot}
            response]
        (if (seq stopping-causes)
          response
          (recur (inc iteration-nb) result-snapshot))))))
