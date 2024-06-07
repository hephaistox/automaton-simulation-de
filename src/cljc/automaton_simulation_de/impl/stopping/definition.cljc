(ns automaton-simulation-de.impl.stopping.definition
  "A `stopping-definition` defines what could cause the scheduler to stop.

  * `doc` describes the definition.
  * `id` name of the `stopping-definition`
  * `next-possible?` tells if the next call of the `scheduler` will raise the same error.
  * `stopping-evaluation` is the function to be called to evalute the criteria and decides if stops is true or not.

  Note that only `stopping-definition` with `stopping-evaluation` properly set are callable by users. Others are built-in `stopping-definition` triggering `stopping-cause` in the `simulation-engine` bounded context.

  ![Entities](archi/simulation_engine/stopping_definition.png)"
  (:require
   [automaton-simulation-de.scheduler.snapshot :as sim-de-snapshot]))

(def id-schema :keyword)

(def schema
  [:map {:closed true}
   [:doc :string]
   [:id id-schema]
   [:next-possible? :boolean]
   [:stopping-evaluation {:optional true}
    [:function
     [:=>
      [:cat sim-de-snapshot/schema [:map]]
      [:map {:closed true}
       [:stop? :boolean]
       [:context [:map]]]]]]])
