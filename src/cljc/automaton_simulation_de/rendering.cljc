(ns automaton-simulation-de.rendering
  "Temporary namespace, work in progress"
  (:require
   [automaton-core.utils.keyword :as utils-keyword]
   [clojure.string :as str]))

(defn evt-str
  "Turns an event in a string

  Params:
  * `evt` an event"
  [{:keys [type date]}]
  (str (utils-keyword/unkeywordize type) "(" date ")"))

(defn evt-with-data-str
  "Turns an event in a string, including its data

  Params:
  * `evt` an event"
  [{:keys [type date]
    :as e}]
  (apply str
         (utils-keyword/unkeywordize type)
         (concat ["("]
                 (str/join ","
                           (cons date
                                 (map utils-keyword/unkeywordize
                                      (vals (apply dissoc e [:type :date])))))
                 [")"])))

(defn scheduler-iteration
  [state-rendering
   {:keys [id state future-events]
    :as scheduler-iteration}]
  (let [evt (first future-events)]
    (println id
             ", " (evt-str evt)
             " -> evts= " (pr-str future-events)
             ", " (if state-rendering (state-rendering state) (pr-str state)))
    scheduler-iteration))
