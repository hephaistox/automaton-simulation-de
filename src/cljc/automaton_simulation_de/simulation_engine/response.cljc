(ns automaton-simulation-de.simulation-engine.response
  "Is a response of scheduler.

  It contains:
  * `stopping-causes`
  * `snapshot`."
  (:require
   [automaton-simulation-de.simulation-engine
    :as-alias sim-engine]
   [automaton-simulation-de.simulation-engine.impl.built-in-sd.causality-broken
    :as sim-de-causality-broken]
   [automaton-simulation-de.simulation-engine.impl.stopping.cause
    :as sim-de-stopping-cause]
   [automaton-simulation-de.simulation-engine.snapshot
    :as sim-de-snapshot]))

(def schema
  [:map {:closed false}
   [::sim-engine/stopping-causes [:sequential sim-de-stopping-cause/schema]]
   [::sim-engine/snapshot sim-de-snapshot/schema]])

(defn add-stopping-cause
  "Adds map `m` among `stop-causes`."
  [response
   {::sim-engine/keys [stopping-causes]
    :as m}]
  (cond-> response
    (and (nil? m) (empty? stopping-causes)) (assoc ::sim-engine/stopping-causes
                                                   [])
    (some? m) (update ::sim-engine/stopping-causes conj m)))

(defn consume-first-event
  "Moves the `response` to the next operation. `current-event` is added to the `stopping-cause` if causality is broken.
  Consists in:

  * removing the first event in the `future-events` and push it in `past-event`.
  * adding `::sim-engine/causality-broken` `stopping-cause` if that event is coming back to the past.
  Note that in case causality is broken, the date is not modified and keep the previous snapshot date instead of the faulty event date."
  [{::sim-engine/keys [snapshot]
    :as response}
   current-event]
  (let [{::sim-engine/keys [date]} snapshot
        next-snapshot (sim-de-snapshot/consume-first-event snapshot)
        causality-broken (sim-de-causality-broken/evaluates snapshot
                                                            next-snapshot
                                                            current-event)]
    (cond-> (assoc response
                   ::sim-engine/snapshot
                   (sim-de-snapshot/next-iteration next-snapshot))
      (some? causality-broken) (add-stopping-cause causality-broken)
      (some? causality-broken)
      (assoc-in [::sim-engine/snapshot ::sim-engine/date] date))))

(defn add-current-event-to-stopping-causes
  "Adds `current-event` to `stopping-causes` in the `response`."
  [response current-event]
  (-> response
      (update ::sim-engine/stopping-causes
              (partial map
                       #(assoc % ::sim-engine/current-event current-event)))))
