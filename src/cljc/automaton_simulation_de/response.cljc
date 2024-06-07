(ns automaton-simulation-de.response
  "Is a response of scheduler.

  It contains:
  * `stopping-causes`
  * `snapshot`."
  (:require
   [automaton-core.adapters.schema                            :as core-schema]
   [automaton-simulation-de.impl.built-in-sd.causality-broken
    :as sim-de-causality-broken]
   [automaton-simulation-de.impl.stopping.cause
    :as sim-de-stopping-cause]
   [automaton-simulation-de.scheduler.snapshot                :as
                                                              sim-de-snapshot]))

(def schema
  [:map {:closed false}
   [::stopping-causes [:sequential sim-de-stopping-cause/schema]]
   [::snapshot sim-de-snapshot/schema]])

(defn build
  "Creates a response with the `stopping-causes` and `snapshot`."
  [stopping-causes snapshot]
  {::stopping-causes (if (nil? stopping-causes) [] stopping-causes)
   ::snapshot snapshot})

(defn validate [response] (core-schema/validate-data-humanize schema response))

(defn add-stopping-cause
  "Adds map `m` among `stop-causes`."
  [response m]
  (if (nil? m) response (update response ::stopping-causes conj m)))

(defn consume-first-event
  "Moves the `response` to the next operation. `current-event` is added to the `stopping-cause` if causality is broken.
  Consists in:

  * removing the first event in the `future-events` and push it in `past-event`.
  * adding `:causality-broken` `stopping-cause` if that event is coming back to the past.
  Note that in case causality is broken, the date is not modified and keep the previous snapshot date instead of the faulty event date."
  [{::keys [snapshot]
    :as response}
   current-event]
  (let [{::sim-de-snapshot/keys [date]} snapshot
        next-snapshot (sim-de-snapshot/consume-first-event snapshot)
        causality-broken (sim-de-causality-broken/evaluates snapshot
                                                            next-snapshot
                                                            current-event)]
    (cond-> (assoc response
                   ::snapshot
                   (sim-de-snapshot/next-iteration next-snapshot))
      (some? causality-broken) (add-stopping-cause causality-broken)
      (some? causality-broken) (assoc-in [::snapshot ::sim-de-snapshot/date]
                                date))))

(defn add-current-event-to-stopping-causes
  "Adds `current-event` to `stopping-causes` in the `response`."
  [response current-event]
  (-> response
      (update ::stopping-causes
              (partial map #(assoc % :current-event current-event)))))
