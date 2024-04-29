(ns automaton-simulation-de.event-library.common
  "Event library helpers."
  (:refer-clojure :exclude [nth])
  (:require
   [automaton-simulation-de.scheduler.event        :as sim-de-event]
   [automaton-simulation-de.scheduler.event-return :as sim-de-event-return]))

(defn sink
  "A sink is a noop event creating no other events."
  [_event state future-events]
  (sim-de-event-return/build state future-events))

(defn init-events
  "Add events from `events-to-add` to the future-events, set the `initial-date` to them."
  [events-to-add initial-date]
  (fn [_event state future-events]
    (sim-de-event-return/build
     state
     (-> future-events
         (concat (map #(assoc % ::sim-de-event/date initial-date)
                      events-to-add))))))

(defn delay-event
  "Play the event in the future, with a fix delay."
  [event-to-postpone delay]
  (fn [{:keys [::sim-de-event/date]} state future-events]
    (let [new-date (+ date delay)]
      (-> (sim-de-event-return/build state future-events)
          (sim-de-event-return/add-event event-to-postpone new-date)))))
