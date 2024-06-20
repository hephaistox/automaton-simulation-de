<!--toc:start-->
- [Entity](#entity)
- [Note:](#note)
<!--toc:end-->

# Entity

In simulation, an entity is a part of the model with a lifecycle.

An entity is based on a model of simulation-engine, so you need to create one first.
```clojure
(def state0
   {})
```

Then, you can create an entity whose name is `:my-first-entity` and whose current state value is the map:

```clojure
(require '[automaton-simulation-de.entity :as sim-entity])
(def state1
  (-> state0
      (sim-entity/create 3
                         :my-first-entity {:data :of
                                           :an :entity})))
```
                                           
As it has a lifecycle, you can query it, it returns `nil` on all non-existing entities:

```clojure
(sim-entity/is-created? state1 :non-existing)
;; nil
(sim-entity/is-living? state1 :non-existing)
;; nil
(sim-entity/is-disposed? state1 :non-existing)
;; nil
```

And for the already created entity, it is both created and living:
```clojure
(sim-entity/is-created? state1 :my-first-entity)
;; #:automaton-simulation-de.entity{:date 3}
(sim-entity/is-living? state1 :my-first-entity)
;; #:automaton-simulation-de.entity{:date 3}
(sim-entity/is-disposed? state1 :my-first-entity)
;; nil
```

You can also update its state value, note this is updating the living lifecycle status:
```clojure
(def state2
  (-> state1
      (sim-entity/update 5
                         :my-first-entity
                         assoc :and :another-data)))
(sim-entity/state state2 :my-first-entity)
;; {:data :of, :an :entity, :and :another-data}
(sim-entity/is-living? state2 :my-first-entity)
;; #:automaton-simulation-de.entity{:date 5}
```

And then get that value back:
```clojure
(-> state2
    (sim-entity/state :my-first-entity))
;;-> {:data :of
;;    :an :entity
;;    :and :other-data}
```

But a life cycle means also the disposal of the entity can happen, so the `state` vanishes, and the entity is not marked living anymore but disposed:
```clojure
(def state3
  (-> state2
      (sim-entity/dispose 12 :my-first-entity)))
(-> state3
    (sim-entity/state :my-first-entity))
;; nil
(-> state3
    (sim-entity/is-living? :my-first-entity))
;; nil
(-> state3
    (sim-entity/is-disposed? :my-first-entity))
;;#:automaton-simulation-de.entity{:date 12}
```

Not all actions make sense on all entity lifecycle status. For instance, updating a disposed event will generate an error:
```clojure
(def state4
  (-> state3
      (sim-entity/update 10
                         :my-first-entity
                         assoc :is :problematic-update)))

(sim-entity/errors state4)
{:my-first-entity #::sim-entity{:why ::sim-entity/updating-a-disposed-entity,
                                :state #::sim-entity{:entities {:my-first-entity
                                                                #::sim-entity{:created #::sim-entity{:date 3},
                                                                              :living #::sim-entity{:date 5},
                                                                              :disposed #::sim-entity{:date 12}}}}
                                :date 10,
                                :entity-name :my-first-entity,
                                :function assoc,
                                :args (:is :problematic-update)}}

(sim-entity/errors state4 :my-first-entity)
```
You can see the errors with the errors function. It returns the errors of one specific entity if it is supplied, or a map associating a collection of error with the name of the entity where the errors happened.

# Note:

An event could have an entity field that stores its name. It's not a concern of the simulation engine to deal with entity creation.
