(ns automaton-simulation-de.middleware.response
  "Is a response of an handler.
  It contains:
  * `stop`
  * `scheduler-snapshot`."
  (:require
   [automaton-simulation-de.scheduler.snapshot :as sim-de-snapshot]))

(defn schema
  []
  [:map {:closed false}
   [::stop [:sequential :map]]
   [::snapshot (sim-de-snapshot/schema)]])

(defn build
  "Creates a response with the `stop` ans `snapshot`."
  [stop snapshot]
  {::stop stop
   ::snapshot snapshot})

(defn prepare
  "Prepares a response based on the snapshot before the iteration

  Returns a response containing the next snapshot and the stop coming from the request.
  A stop condition is added if the `next-snapshot` happens before the current `previous-snapshot`."
  [previous-snapshot stop]
  (let [next-snapshot (sim-de-snapshot/next-snapshot previous-snapshot)]
    (build (cond-> (if (nil? stop) [] stop)
             (pos? (compare (::sim-de-snapshot/date previous-snapshot)
                            (::sim-de-snapshot/date next-snapshot)))
             (conj {:cause ::causality-broken
                    :previous-date (::sim-de-snapshot/date previous-snapshot)
                    :next-date (::sim-de-snapshot/date next-snapshot)}))
           next-snapshot)))

(defn add-stop
  "Add map `m` among stop causes."
  [response m]
  (update response ::stop conj m))

(defn add-current-event
  "Adds current event to stop reasons."
  [response current-event]
  (cond-> response
    (some? current-event)
    (update ::stop (partial map #(assoc % :current-event current-event)))))
