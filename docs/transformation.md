# Transformation

Transformation contains useful functions that modify simulation output to feed the rendering.


## Simulation engine filtering
When the user needs to focus on a sub-model, the simulation transformation provides useful functions in `automaton-simulation-de.transformation` ns to limit the information provided to the rendering

```clojure
(require '[automaton-simulation-de.transformation :as sim-trans]
         '[automaton-simulation-de.predicates :as sim-pred])

(1) (def predicate [::sim-pred/equal? :main-color :blue] )

(2) (def state {:m1 {:name :m1
                     :mix-color {:a :blue}
                     :main-color :green}
                :m2 {:name :m2
                     :main-color :blue}
                :m3 {:name :m3
                     :main-color :red}
                :m4 {:name :m4
                     :main-color :blue}})
                     
(3) (-> predicate 
    sim-pred/predicate-query->pred-fn 
    (sim-trans/keep-state state)
    ;; => {:m2 {:name :m2 :main-color :blue}
    ;;     :m4 {:name :m4 :main-color :blue}}
```

(1) Predicate is a filter selected by the user, here it checks if under :main-color the value is :blue (read more about it in [predicates](predicates.md))

(2) We define a state that will be transformed 

(3) `keep-state`  As a first parameter, expect a predicate-fn that will be used to decide if an element of a state should be kept or removed. The second parameter is expected to be the state itself.
   
so as a result we've got :m2 and :m4 in state which has that value in their maps.


```clojure 
(def predicate-fn (predicate-query->pred-fn [::sim-trans/not [::sim-trans/is-empty :machine]]))

(def past-events [{:type :IN}
                  {:type :AR
                   :machine :m1
                   :product :p1}
                  {:type :MR
                   :machine :m1
                   :product :p1}
                  {:type :PR
                   :product :p1}])

(sim-trans/keep-events predicate-fn past-events)
  ;;  => [{:type :AR
  ;;       :machine :m1
  ;;       :product :p1}
  ;;       {:type :MR
  ;;        :machine :m1
  ;;        :product :p1}]
```

`keep-events` function is similar to keep-state, with the difference it is meant for filtering past/future events collections. It will pass to predicate-fn each event and keep only those that match the predicate

In this example, our predicate returns true if an event will have any value under :machine, which leaves us with two events


```clojure 
(def predicate-fn (predicate-query->pred-fn [::sim-trans/is :main-color :blue]))

(def snapshot
  #:automaton-simulation-de.simulation-engine{
    :date 5
    :id 20
    :iteration 20
    :state {:m1 {:name :m1
                 :mix-color {:a :blue}
                 :main-color :green}
            :m2 {:name :m2
                 :main-color :blue}
            :m3 {:name :m3
                 :main-color :red}
            :m4 {:name :m4
                 :main-color :blue}}
    :past-events [{:type :IN}
                  {:type :AR
                   :machine :m1
                   :product :p1}
                  {:type :MR
                   :machine :m1
                   :product :p1}
                  {:type :PR
                   :product :p1}]
    :future-events [{:type :AR
                     :machine :m2
                     :product :p2}]})
                     
(3) (sim-trans/keep-snapshot-state predicate-fn state)
    ;;=> #:automaton-simulation-de.simulation-engine{
    ;;     :date 5
    ;;     :id 20
    ;;     :iteration 20
    ;;     :state {:m2 {:name :m2
    ;;                  :main-color :blue}
    ;;             :m4 {:name :m4
    ;;                  :main-color :blue}}
    ;;     :past-events [{:type :IN}
    ;;                   {:type :AR
    ;;                    :machine :m1
    ;;                    :product :p1}
    ;;                   {:type :MR
    ;;                    :machine :m1
    ;;                    :product :p1}
    ;;                   {:type :PR
    ;;                    :product :p1}]
    ;;     :future-events [{:type :AR
    ;;                      :machine :m2
    ;;                      :product :p2}]})
```

`keep-snapshot-state` is similar to `keep-snapshot, with the difference that it knows how to filter state inside of a snapshot. As a result, we have a snapshot that has all other fields untouched, except ::sim-engine/state, which was filtered accordingly to the predicate-fn


```clojure 
(def predicate-fn (predicate-query->pred-fn [::sim-trans/is :main-color :blue]))

(def snapshot
  #:automaton-simulation-de.simulation-engine{
    :date 5
    :id 20
    :iteration 20
    :state {:m1 {:name :m1
                 :mix-color {:a :blue}
                 :main-color :green}
            :m2 {:name :m2
                 :main-color :blue}
            :m3 {:name :m3
                 :main-color :red}
            :m4 {:name :m4
                 :main-color :blue}}
    :past-events [{:type :IN}
                  {:type :AR
                   :machine :m1
                   :product :p1}
                  {:type :MR
                   :machine :m1
                   :product :p1}
                  {:type :PR
                   :product :p1}]
    :future-events [{:type :AR
                     :machine :m2
                     :product :p2}]})
                     
(3) (sim-trans/keep-snapshot-events-based-state predicate-fn :machine state)
    ;;=> #:automaton-simulation-de.simulation-engine{
    ;;     :date 5
    ;;     :id 20
    ;;     :iteration 20
    ;;     :state {:m2 {:name :m2
    ;;                  :main-color :blue}
    ;;             :m4 {:name :m4
    ;;                  :main-color :blue}}
    ;;     :past-events []
    ;;     :future-events [{:type :AR
    ;;                      :machine :m2
    ;;                      :product :p2}]})
```

`keep-snapshot-events-based-state` is similar to keep-snapshot-state, but after filtering state it also filters all events based on state keys that are left (here `:m2` `:m4`), expecting one of those keys as a value to have under the path supplied with second parameter (:machine). 

This resulted in filtering out all past-events, as none of them had :m2 or :m4 under :machine key and keeping all future events.

## RC / Entities .... 
There are analogical functions covering entities/rc concepts
