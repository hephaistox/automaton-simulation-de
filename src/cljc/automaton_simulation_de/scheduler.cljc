(ns automaton-simulation-de.scheduler
  "Scheduler aggregate details can be found in `docs/archi/scheduler-details.mermaid`, it's global context in `docs/archi/scheduler-context.mermaid` and a timeline in `docs/archi/scheduler-timeline.mermaid`"
  (:require
   [automaton-build.schema :as build-schema]
   [automaton-core.log :as core-log]
   [automaton-simulation-de.event-registry :as sim-de-event-registry]
   [automaton-simulation-de.scheduler.iteration :as sim-de-scheduler-iteration]
   [automaton-simulation-de.scheduler.middleware
    :as
    sim-de-scheduler-middleware]))

(defn scheduler
  "The scheduler is creating the next scheduler iteration, until the stopping criteria are met. It is executing scheduler middleware after each of scheduler iteration.

  Stopping criteria is checked by looking for `:stop`` key in scheduler-iteration state being `true`.

  Params:
  * `event-registry` a map complying to event-registry-schema
  * `initial-scheduler-iteration` - map complying to `scheduler-iteration-schema` to start with
  * `scheduler-middleware` - sequence of functions to execute after scheduler-iteration, each function receives the last scheduler-iteration and is expected to return it.

  Returns last scheduler-iteration"
  [event-registry initial-scheduler-iteration scheduler-middleware]
  (let [scheduler-middleware
        (sim-de-scheduler-middleware/global-stopping-criteria
         scheduler-middleware)
        event-registry (build-schema/add-default-values
                        sim-de-event-registry/event-registry-schema
                        event-registry)
        event-registry-kvs (sim-de-event-registry/event-registry-kvs
                            event-registry)
        event-ordering-fn (sim-de-event-registry/event-ordering-fn
                           event-registry)]
    (cond
      (nil? (build-schema/valid?
             sim-de-scheduler-iteration/scheduler-iteration-schema
             initial-scheduler-iteration
             "Scheduler iteration"))
      (do (core-log/error-data initial-scheduler-iteration
                               "Scheduler iteration is not valid")
          initial-scheduler-iteration)
      (nil? (build-schema/valid? sim-de-event-registry/event-registry-schema
                                 event-registry
                                 "Event registry"))
      (do (core-log/error-data initial-scheduler-iteration
                               "Event registry is not valid")
          initial-scheduler-iteration)
      :else (loop [scheduler-iteration initial-scheduler-iteration]
              (let [{:keys [state]
                     :as next-scheduler-iteration}
                    (-> (sim-de-scheduler-iteration/execute event-registry-kvs
                                                            event-ordering-fn
                                                            scheduler-iteration)
                        (sim-de-scheduler-middleware/execute
                         scheduler-middleware))]
                (if (:stop state)
                  next-scheduler-iteration
                  (recur next-scheduler-iteration)))))))
