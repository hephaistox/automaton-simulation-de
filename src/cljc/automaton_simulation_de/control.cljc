(ns automaton-simulation-de.control
  "Control is a higher level API for simulation scheduler. With a control state you have a pointer to one iteration that you can control with play!, next!, move-x ...

   It's design documetns are in docs/archi/control/...

   Design decision:
   Control state as an atom containing all information about rendering

   Pros:
   - With atom state can be managed during long-lasting operations
     e.g. during play! we can modify the speed of it, modify pause easily
   - We can reuse atom state to modify the flow from multiple places
   - Atom handles well multithreading
   - Atom can be used both on clj and cljs
   Cons:
   - Not known

   Design decision:
   Split between control and computation

   Pros:
   - Split allows to easier manage responsibility between controling what's current response to render and how to move between them
   - Computation being separated allows for flexibility in how to manage execution of scheduler. This allows for adding later on optimisation, split between backend/frontend rendering, offline/online usag...
  Cons:
   - User of rendering needs to have more knowledge about how rendering is working

   Design decision:
   Control based on computation (vs e.g. control based on state and separated from computation)
   Pros:
   - Code is easier to manage and understand (decrease complexity)
   - We are in rendering context so it allows for better solutions for rendering
   Cons:
   - Control knows about computation so code is more entangled"
  (:require
   [automaton-simulation-de.control.computation
    :as sim-de-computation]
   [automaton-simulation-de.control.computation.registry
    :as sim-de-computation-registry]
   [automaton-simulation-de.control.state
    :as sim-de-rendering-state]
   [automaton-simulation-de.impl.stopping-definition.state-contains
    :as sim-de-sc-state-contains]
   #?(:clj [clojure.core.async :refer [<! go-loop timeout]]
      :cljs [cljs.core.async :refer [<! timeout]]))
  #?(:cljs (:require-macros [cljs.core.async.macros :refer [go-loop]])))

(defn- next-iteration-nb
  [snapshot x]
  (let [curr-it (or (get-in
                     snapshot
                     [:automaton-simulation-de.response/snapshot
                      :automaton-simulation-de.scheduler.snapshot/iteration])
                    0)
        new-it-nb (+ curr-it x)]
    (if (< new-it-nb 0) 0 new-it-nb)))

(defn wrap-registry
  "Wraps a model to add necessary behavior to model rendering.
   Adds stopping-criteria that are usefull to manage state"
  [registry]
  (update registry
          :stopping
          merge
          {:state-contains (sim-de-sc-state-contains/stopping-definition)}))

(defn build-rendering-state
  "Builds state atom that is required to use controls"
  [initial-state]
  (sim-de-rendering-state/build initial-state))

(defn make-computation
  "Creates computation object for rendering state to manage computation of scheduler responses"
  [model computation-type & comp-data]
  (apply (get (sim-de-computation-registry/computation-registry)
              computation-type)
         model
         comp-data))

(defn all-stopping-criterias
  "All stopping criterias that are possible"
  [state]
  (sim-de-computation/stopping-criterias (:computation
                                          (sim-de-rendering-state/get state))))

(defn move-x!
  "Move `x` number of iterations from the point of state `current-iteration`
   Params:
   `x` - integer (both negative and positive numbers)"
  [state x]
  (when (and state (:computation (sim-de-rendering-state/get state)))
    (let [it-nb (next-iteration-nb (:current-iteration
                                    (sim-de-rendering-state/get state))
                                   x)
          iteration (sim-de-computation/iteration-n
                     (:computation (sim-de-rendering-state/get state))
                     it-nb)]
      (when (= :success (:automaton-simulation-de.control/status iteration))
        (sim-de-rendering-state/set state
                                    :current-iteration
                                    (:automaton-simulation-de.control/response
                                     iteration)))
      iteration)))

(defn pause!
  "Set :pause? in `state` to `val` boolean
   Default value for `val` is opposite of current value
   Especially useful for stopping `play!` fn`"
  ([state val]
   (:current-iteration (sim-de-rendering-state/set state :pause? val)))
  ([state] (pause! state (not (:pause? (sim-de-rendering-state/get state))))))

(defn- sleep [sleep-ms] (<! (timeout sleep-ms)))

(defn- play*
  "Starting with `current-response` goes to next iteration response and pass it to `on-iteration-fn` with interval defined by `play-delay-fn` untill there is no next iteration possible or `pause?-fn` evaluates to true. When finished executes `on-finish-fn`"
  [computation snapshot pause?-fn play-delay-fn on-iteration-fn on-finish-fn]
  (when computation
    (go-loop [next-iteration (sim-de-computation/iteration-n
                              computation
                              (next-iteration-nb snapshot 1))]
      (cond
        (pause?-fn next-iteration) (on-finish-fn next-iteration)
        (= :no-next (:automaton-simulation-de.control/status next-iteration))
        (do (on-iteration-fn next-iteration) (on-finish-fn next-iteration))
        :else (do (on-iteration-fn next-iteration)
                  (sleep (play-delay-fn))
                  (recur (sim-de-computation/iteration-n
                          computation
                          (next-iteration-nb
                           (:automaton-simulation-de.control/response
                            next-iteration)
                           1))))))))

(defn play!
  "Render simulation until rendering is paused or impossible to go further.
   On each iteration executes `on-iteration-fn` with scheduler response.
   Speed of going to next iteration can be adjusted with `:play-delay` in `state`"
  [state on-iteration-fn]
  (when (and state (:computation (sim-de-rendering-state/get state)))
    (let [on-iteration-fn #(do (sim-de-rendering-state/set
                                state
                                :current-iteration
                                (:automaton-simulation-de.control/response %))
                               (on-iteration-fn %))]
      (pause! state false)
      (play* (:computation (sim-de-rendering-state/get state))
             (:current-iteration (sim-de-rendering-state/get state))
             (partial (fn [state _]
                        (:pause? (sim-de-rendering-state/get state)))
                      state)
             #(:play-delay (sim-de-rendering-state/get state))
             on-iteration-fn
             #(pause! state true)))))

(defn fast-forward?
  "Is fast forward possible?"
  [state]
  (sim-de-computation/stopping-criteria-model-end?
   (:computation (sim-de-rendering-state/get state))))

(defn fast-forward!
  "Move to last possible iteration"
  [state]
  (when (and state (:computation (sim-de-rendering-state/get state)))
    (let [iteration (sim-de-computation/model-end-iteration
                     (:computation (sim-de-rendering-state/get state)))]
      (sim-de-rendering-state/set state
                                  :current-iteration
                                  (:automaton-simulation-de.control/response
                                   iteration))
      iteration)))

(defn rewind!
  "Move to first iteration"
  [state]
  (when (and state (:computation (sim-de-rendering-state/get state)))
    (let [iteration (-> (sim-de-rendering-state/get state)
                        :computation
                        (sim-de-computation/iteration-n 0))]
      (when (= :success (:automaton-simulation-de.control/status iteration))
        (sim-de-rendering-state/set state
                                    :current-iteration
                                    (:automaton-simulation-de.control/response
                                     iteration)))
      iteration)))
