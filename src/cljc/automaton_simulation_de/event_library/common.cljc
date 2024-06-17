(ns automaton-simulation-de.event-library.common
  "Event library helpers."
  (:require
   [automaton-simulation-de.simulation-engine              :as-alias sim-engine]
   [automaton-simulation-de.simulation-engine.event-return
    :as sim-de-event-return]))

(defn sink
  "A sink is a noop event creating no other events."
  [_event state future-events]
  #:automaton-simulation-de.simulation-engine{:state state
                                              :future-events future-events})

(defn init-events
  "Add events from `events-to-add` to the future-events, set the `initial-date` to them."
  [events-to-add initial-date]
  (fn [_event state future-events]
    #:automaton-simulation-de.simulation-engine{:state state
                                                :future-events
                                                (-> future-events
                                                    (concat (map
                                                             #(assoc
                                                               %
                                                               ::sim-engine/date
                                                               initial-date)
                                                             events-to-add)))}))

(defn delay-event
  "Play the event in the future, with a fix delay."
  [event-to-postpone delay]
  (fn [{::sim-engine/keys [date]} state future-events]
    (let [new-date (+ date delay)]
      (-> #:automaton-simulation-de.simulation-engine{:state state
                                                      :future-events
                                                      future-events}
          (sim-de-event-return/add-event event-to-postpone new-date)))))
