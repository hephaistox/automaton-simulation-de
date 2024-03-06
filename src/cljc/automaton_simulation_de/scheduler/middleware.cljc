(ns automaton-simulation-de.scheduler.middleware
  "Temporary namespace, work in progress"
  (:require
   [automaton-core.log :as core-log]))

(def ^:private infinite-loop-detection-max-iter
  "Maximum number of iteration to detect infinite loop"
  1000000000)

(defn stop-iteration
  [{:keys [state]
    :as scheduler-iteration}]
  (assoc scheduler-iteration :state (assoc state :stop true)))

(defn endless-simulation-stopping-criteria
  [{:keys [id]
    :as scheduler-iteration}]
  (if (>= id infinite-loop-detection-max-iter)
    (do
      (core-log/error
       "The simulation has stopped as the higher max simulation has been reached")
      (stop-iteration scheduler-iteration))
    scheduler-iteration))

(defn no-future-events-stopping-criteria
  [{:keys [future-events]
    :as scheduler-iteration}]
  (if (empty? future-events)
    (do
      (core-log/info
       "The simulation has stopped as there is no more future events to execute")
      (stop-iteration scheduler-iteration))
    scheduler-iteration))


(defn global-stopping-criteria
  [scheduler-middleware]
  (conj (vec scheduler-middleware)
        endless-simulation-stopping-criteria
        no-future-events-stopping-criteria))

(defn execute
  [scheduler-iteration initial-scheduler-middleware]
  (loop [iteration scheduler-iteration
         scheduler-mdw initial-scheduler-middleware]
    (if (empty? scheduler-mdw)
      iteration
      (recur ((first scheduler-mdw) iteration) (rest scheduler-mdw)))))
