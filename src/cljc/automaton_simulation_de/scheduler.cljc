(ns automaton-simulation-de.scheduler
  "Scheduler aggregate details can be found in [scheduler diagram](docs/archi/scheduler-details.svg), its [global context](docs/archi/scheduler-context.svg)

  An event has three parameters `(event-execution current-event state new-future-events)`:
  * `current-event` which is the current event to execute
  * `state` which is the state value before the event execution
  * `new-future-events` which is the list of future events without the current event

  The returned value is a `event-return`, which future events have no needs to be sorted, they will be by the scheduler.

  This `event-return` contains the stop reason, that could be:
  * `::no-future-events` when no future events exists
  * `::causality-broken` when the date of the event is in the past
  * `::execution-not-found` when the type is unknown
  * `::failed-event-execution` when the execution of the event if raising an exception
  * `::nil-handler` when the handler is not given
  * `::max-iteration-number` the maximum iteartion number is reached


  * [See entity](docs/archi/scheduler_entity.png)
  * [See aggregate](docs/archi/scheduler_aggregate.png)

    ### Causality
    Causality is a property of the simulation model stating that a future event cannot change what has been done in the past already, so all changes in the state should not contradict any past event. For instance, anticipating an event would lead to causality violation."
  (:refer-clojure :exclude [iterate])
  (:require
   [automaton-simulation-de.middleware.request :as sim-de-request]
   [automaton-simulation-de.middleware.response :as sim-de-response]
   [automaton-simulation-de.middlewares :as sim-de-middlewares]
   [automaton-simulation-de.ordering :as sim-de-event-ordering]
   [automaton-simulation-de.registry :as sim-de-event-registry]
   [automaton-simulation-de.scheduler.snapshot :as sim-de-snapshot]))

(defn handler
  "Handler is executing a request and returning a response.

  An handler could be wrapped with middlewares.
  Params:
  * `request`"
  [request]
  (let [{::sim-de-request/keys [stop snapshot event-execution sorting]} request
        {::sim-de-snapshot/keys [state future-events]} snapshot
        [current-event & new-future-events] future-events
        response (sim-de-response/prepare snapshot stop)]
    (cond (seq stop) (update response
                             ::sim-de-response/stop
                             (partial map #(assoc % :current-event current-event)))
          (nil? event-execution) (sim-de-event-registry/add-registry-stop response
                                                                    current-event)
          :else
          (try
            (let [event-return (event-execution current-event state new-future-events)]
              (update response
                      ::sim-de-response/snapshot
                      (partial sim-de-snapshot/update-snapshot-with-event-return event-return sorting)))
            (catch #?(:clj Exception :cljs :default) e
              (sim-de-response/add-stop response
                                        {:cause ::failed-event-execution
                                         :error e}))))))

(defn iterate
  "Iterates on the scheduler, it executes the handler based on data in the snapshot and creates the next one.

  The event that is executed thanks to the code found for that event in the `registry`

  A scheduler iteration should not access past or future events to have some information on the state of the simulation

  Params:
  * `registry` registry mapping event type to event execution
  * `sorting`
  * `snapshot` snapshot that will be used to iterate

  Returns the response, the next snapshot, with state, future and past data updated according to event execution."
  [registry sorting handler snapshot]
  (let [{::sim-de-snapshot/keys [future-events]} snapshot
        current-event (first future-events)
        event-execution (sim-de-event-registry/get registry
                                             current-event)
        {::sim-de-request/keys [stop] :as request} (sim-de-request/prepare event-execution snapshot sorting)]
    (cond (not (fn? handler)) (sim-de-response/prepare snapshot (conj stop {:cause ::nil-handler}))
          :else (handler request))))

(defn scheduler
  "The scheduler is creating the next `scheduler-snapshot`, until the `stopping criteria` are met. It is executing scheduler middleware with each scheduler snapshot.

  For each `scheduler-snapshot` created, the scheduler is sorting the future events based on the event `ordering` defined in the `event-registry`. The first element in ordering is sorted first, in case of tie, the second order is used, and so on.
  The first `event` of that order is the current event, this event is executed (see `event-execution`). The resulting new values of state, past events, future events are used to create the new `scheduler-snapshot`.

  Stopping criteria is checked by looking for `:stop` key in scheduler-iteration state being `true`.

  Params:
  * `registry` registry mapping event type to event execution.
  * `middlewares` list of middlewares, executed from left to right for `request` and from right to left for `response`.
  * `ordering` sort two events.
  * `initial-snapshot` the snaphost to start with.
  * `max-iteration` maximum iteration number to limit endless simulation.

  Returns last `snapshot`"
  [registry middlewares ordering initial-snapshot max-iteration]
  (let [sorting (sim-de-event-ordering/sorter ordering)
        wrapped-handler (sim-de-middlewares/wrap-handler handler middlewares)
        sorted-snapshot (update initial-snapshot
                                ::sim-de-snapshot/future-events sorting)]
    (loop [iteration-nb 1
           snapshot sorted-snapshot]
      (let [{::sim-de-response/keys [stop snapshot] :as response} (iterate registry sorting wrapped-handler snapshot)]
        (cond (seq stop) response
              (>= iteration-nb max-iteration) (sim-de-response/add-stop response
                                                                        {:cause ::max-iteration-number
                                                                         :max-iteration max-iteration
                                                                         :iteration iteration-nb})
              :else (recur (inc iteration-nb) snapshot))))))
