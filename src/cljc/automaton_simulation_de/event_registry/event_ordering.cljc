(ns automaton-simulation-de.event-registry.event-ordering
  "Event ordering is a part of the event registry meant to sort the future events"
  (:require
   [automaton-simulation-de.event :as sim-de-event]))

(defn default-date-ordering
  "Default way to sort dates, assumes that date is comparable by `<``"
  [e1 e2]
  (let [d1 (:date e1) d2 (:date e2)] (when (not= d1 d2) (< d1 d2))))

(defn default-same-date-ordering
  "Compares two events
   Params:
  * `evt-type-priority` list of event priorities
  * `e1` first event
  * `e2` second event
  Returns true if `e1` is before `e2`"
  [evt-type-priority e1 e2]
  (cond
    (not= (:type e1) (:type e2)) (< (.indexOf evt-type-priority (:type e1))
                                    (.indexOf evt-type-priority (:type e2)))
    :else (< (hash e1) (hash e2))))

(def event-ordering-schema
  "date-ordering is sorting events by date.
   same-date-ordering is for sorting simultaneous events"
  [:map {:closed true}
   [:date-ordering {:default default-date-ordering}
    [:=>
     [:cat sim-de-event/event-schema sim-de-event/event-schema]
     [:maybe :boolean]]]
   [:same-date-ordering
    [:=>
     [:cat sim-de-event/event-schema sim-de-event/event-schema]
     [:maybe :boolean]]]])

(defn event-ordering
  "Orders events by date, if events have the same date (`date-ordering` returns nil), than they are sorted with `same-date-ordering`.
  Applys caller defined decision on which event should be executed first"
  [{:keys [date-ordering same-date-ordering]} e1 e2]
  (if (nil? (date-ordering e1 e2))
    (same-date-ordering e1 e2)
    (date-ordering e1 e2)))
