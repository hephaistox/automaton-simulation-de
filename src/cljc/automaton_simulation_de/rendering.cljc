(ns automaton-simulation-de.rendering
  "Temporary namespace, work in progress"
  (:require
   [automaton-core.utils.keyword            :as core-keyword]
   [automaton-simulation-de.scheduler.event :as sim-de-event]
   [clojure.string                          :as str]))

(defn evt-str
  "Turns an event `evt` into a string."
  [{:keys [::sim-de-event/date ::sim-de-event/type]
    :as _evt}]
  (str (core-keyword/unkeywordize type) "(" date ")"))

(defn evt-with-data-str
  "Turns an event `evt` into a string, including its data."
  [{:keys [::sim-de-event/date ::sim-de-event/type]
    :as evt}]
  (apply str
         (core-keyword/unkeywordize type)
         (concat ["("]
                 (str/join
                  ","
                  (cons date
                        (-> (dissoc evt ::sim-de-event/date ::sim-de-event/type)
                            vals)))
                 [")"])))

(defn scheduler-snapshot
  [state-rendering
   {:keys [id state future-events]
    :as scheduler-iteration}]
  (let [evt (first future-events)]
    (println id
             ", " (evt-str evt)
             " -> evts= " (pr-str future-events)
             ", " (if state-rendering (state-rendering state) (pr-str state)))
    scheduler-iteration))
