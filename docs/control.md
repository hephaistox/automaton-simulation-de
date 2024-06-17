# Control 
Simulation control can be viewed as an API for accessing information about Simulation.

It was done with rendering use-case in mind, where you need to display specific iteration information and move between iterations to gather more information (e.g. see next, see previous...)
  
This tutorial will speak about the usage of control and a basic overview of it.

## Quickstart
First, to start we need to create the control state, that will be leveraged to keep information specific to our control.

We will use toy-example model data to create a computation instance that the control will leverage to get information about the simulation. Don't worry about its specificity it's not important for now.

```clojure
(ns control-demo)

(require '[automaton-simulation-de.demo.control :as toy-example]
         '[automaton-simulation-de.control :as sim-de-control]
         '[automaton-simulation-de.control.state :as sim-de-control-state])
         
(def computation (sim-de-control/make-computation (toy-example/model) :direct))
(def state
    (sim-de-control/build-rendering-state {:computation computation})) 

```
 
And now let's use that state to start the simulation:

```clojure
(sim-de-control/move-x state 1)
=> 
#:automaton-simulation-de.control{:response {...}
                                  :status :success}
```
We received a control response, status `:success` means it was able to get what we asked for.
And `response` contains the scheduler response we asked for (which contains of snapshot and stopping-causes).
So let's zoom in on the snapshot iteration-number to get a better grasp of how control works. 

```clojure 
(defn- snapshot-it-nb
  [control-response]
  (get-in control-response
          [::sim-de-control/response
           ::sim-engine/snapshot
           ::sim-engine/iteration]))

(snapshot-it-nb (sim-de-control/move-x state 1))
=> 2
(snapshot-it-nb (sim-de-control/move-x state 1))
=> 3
(snapshot-it-nb (sim-de-control/move-x state 5))
=> 8
(snapshot-it-nb (sim-de-control/move-x state -3))
=> 5
```
Move-x is moving from the current iteration to the `iteration + num` that we specify

To know what is the current iteration you can also look at the state we are passing:
```clojure
(snapshot-it-nb (:current-iteration (sim-de-control-state/get state)))
=>
5
```


Another function in control is play! and pause!
```clojure
(sim-de-control/play! state #(prn (snapshot-it-nb %)))
=>
6
*after 1 second*
7
*after 1 second*
8
*after 1 second*
9
*after 1 second*
10

(sim-de-control/pause! state)

(snapshot-it-nb (:current-iteration (sim-de-control-state/get state)))
=>
10
```


The first argument for play! as always is the state and the second is a function that will be executed in each iteration.

Play is about automatically changing the current iteration to the next (like doing (move-x! state 1)).
By default, it waits 1000 ms before going to the next iteration, it is done to enable a user to see more clearly what's going on in the simulation itself, iteration by iteration.

The speed of going between iterations can be adjusted by the `:play` property in the state

```clojure 
(:play (sim-de-control-state/get state))
=> 
1000

(sim-de-control-state/set state :play 2000)
(sim-de-control/play! state #(prn (snapshot-it-nb %)))
=>
11
* after 2 seconds *
12
---
*simultaneously let's change the pace!*
(sim-de-control-state/set state :play 5000)
---
*after 5 seconds*
13
*after 5 seconds*
14

(sim-de-control/pause! state)
```

You may notice by now, that at the end I always call pause! fn, this is a way to stop the play fn from continuing.

And what it does really is setting `:pause?` on the state to `true`, the pause! fn by itself it does not know anything about play! fn, so setting it to true won't work like starting the play! fn
It is only manipulating the state property.


```clojure
(:pause? (sim-de-control-state/get state))
=>
true 

(sim-de-control/pause! state)

(:pause? (sim-de-control-state/get state))
=>
false

(sim-de-control/pause! state false)

(:pause? (sim-de-control-state/get state))
=> 
false

(sim-de-control/pause! state true)

(:pause? (sim-de-control-state/get state))
=> 
true
```

Another useful function is fast-forward!
Which will move to simulation `::sim-engine/model-end?` if it's not defined in a model it will move until any stopping-criteria is met

```clojure
(first (get-in  [:automaton-simulation-de.control/response
           :automaton-simulation-de.response/stopping-causes]))
=>
{:stopping-criteria {:stopping-definition {... :id ::sim-engine/no-future-events}}}
```

To know what `stopping-criteria` are defined in your model you can run:
```clojure
(sim-de-control/all-stopping-criterias rendering-state)
```
which will return a collection of stopping-criteria defined by modeler


In the control state there is one more property, which is called :computation, that we set at the beginning.
Control comes with a computation registry, that contains computation implementations you can choose from. 
You can also implement your own and just set it in the state under :computation.
You can even change those implementations between executing control commands.

Computation is responsible for executing the simulation, so this is a great place for any optimization, but it is an advanced topic and will not be covered in this tutorial. 

That being said in most of the use-cases the default implementations should be enough.
