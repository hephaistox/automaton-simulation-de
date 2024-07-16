<!-- markdown-toc start - Don't edit this section. Run M-x markdown-toc-refresh-toc -->
**Table of Contents**

- [Transformation](#transformation)
    - [Predicates query language](#predicates-query-language)
        - [Equality predicates](#equality-predicates)
        - [Composed predicates](#composed-predicates)
    - [Predicates advanced](#predicates-advanced)
        - [Adding custom-made predicate](#adding-custom-made-predicate)

<!-- markdown-toc end -->


# Predicates
The Predicates module contains functions that return a boolean or nil if the values can't be compared and a small DSL to represent and apply them. 

## Equality predicates
Equality predicates are predicates that return a boolean based on a comparison of values

```clojure 
(ns transformation-doc)

(require '[automaton-simulation-de.predicates :as sim-pred])

(1) (def my-data {:name :m1
                  :color :blue})

(2) (def pred-intent [::sim-pred/equal? :color :blue])

(3) (sim-pred/apply-predicate-query pred-intent my-data)
;;=> true
```

(1) The data structure to apply the predicate to. 

(2) User predicate in the form of a vector.

The schema for that predicate vector is:
[:predicate-id & :predicate-params]

:predicate-id is expected to match a keyword in the predicates-registry (which is located in sim-pred/predicates-registry). 
If you need custom predicates head to [Adding custom-made predicate]. 

:predicate-params are what will be passed to predicate-fn and can vary in numbers of parameters and value types, depending on the predicate used. 

The predicate we are using here:
 `::sim-pred/equal?` expects two params:
 1. a path in the form of a keyword or vector (here `:color`)
 2. a value to compare the value under the path with (`:blue`)
If the value under a path matches the second value passed to it, it will return true. 
 
(3) This function will take a predicate language query, turn it into a function, and apply to function second parameter (`my-data` (1))

Which has `:blue` under :color so it returns true

```clojure
(def pred-intent [::sim-pred/equal? :blue])
(sim-pred/apply-predicate-query pred-intent my-data) 
;;=> false
(sim-pred/apply-predicate-query pred-intent :blue)
;; => true
(sim-pred/apply-predicate-query [::sim-pred/equal? {:color :blue}] {:color :blue})
;; => true

```
When only one argument is passed to `::sim-pred/equal?` it will compare the whole value passed to it.


```clojure
(def pred-intent [::sim-pred/equal? :color :red])
(sim-pred/apply-predicate-query pred-intent my-data) 
;;   => false 
```
Value under :color is :blue, so asking for :red will return false


```clojure
(def pred-intent [::sim-pred/equal? [:mix :color] :blue])
(sim-pred/apply-predicate-query pred-intent my-data) 
;;   => false 
```
There is nothing under :mix :color path in our data, so it returns false


Most of the predicates expect the same params as `::sim-pred/equal?`

```clojure 
(def pred-intent [::sim-pred/one-of? :color [:blue :red]])

(sim-pred/apply-predicate-query pred-intent {:color :red})
;;=> true

(sim-pred/apply-predicate-query pred-intent {:color :blue})
;;=> true

(sim-pred/apply-predicate-query pred-intent {:color :green})
;;=> false
```
one-of is similar to ::sim-pred/equal?, with the difference that it compares the value under the path with all the values in the collection passed to it and returns true if ANY of them match.

```clojure 
(def pred-intent [::sim-pred/contains? :name "m1"])

(sim-pred/apply-predicate-query pred-intent {:name "m1"})
;;=> true

(sim-pred/apply-predicate-query pred-intent {:name "m1sadw21"})
;;=> true

(sim-pred/apply-predicate-query pred-intent {:name "whatever-m1"})
;;=> true

(sim-pred/apply-predicate-query pred-intent {:name "don't m have it 1"})
;;=> false
```
::sim-pred/contains will return true if the value under the specified path contains the value passed in the argument


```clojure 
(def pred-intent [::sim-pred/contains? :colors :blue])

(sim-pred/apply-predicate-query pred-intent {:colors [:blue]})
;;=> true

(sim-pred/apply-predicate-query pred-intent {:colors '(:green :red :blue)})
;;=> true

(sim-pred/apply-predicate-query pred-intent {:colors [:red :yellow]})
;;=> false 

(sim-pred/apply-predicate-query pred-intent {:colors nil})
;;=> false
```
Contains works on different value types 


```clojure 
(def pred-intent [::sim-pred/true? :turned-on])

(sim-pred/apply-predicate-query pred-intent {:turned-on true})
;;=> true

(sim-pred/apply-predicate-query pred-intent {:turned-on false})
;;=> false

(sim-pred/apply-predicate-query pred-intent {:turned-on "Yes I am"})
;;=> false

(sim-pred/apply-predicate-query pred-intent {:different-key true})
;;=> false
```
true? the predicate that will return true only if there is a boolean true value 

```clojure 
(def pred-intent [::sim-pred/> :capacity 5])

(sim-pred/apply-predicate-query pred-intent {:capacity 6})
;; => true

(sim-pred/apply-predicate-query pred-intent {:capacity 1})
;; => false

(sim-pred/apply-predicate-query pred-intent {:capacity "NaN"})
;; => nil

```
But not all predicates work for all value types, 

::sim-pred/> will expect that it's comparing numbers

If it's not able to compare, it returns nil.


## Composed predicates
But there are also composed predicates, which expect as a parameter one or more predicates and return boolean based on them.

```clojure 
(def pred-intent [::sim-pred/and [::sim-pred/equal? :color :blue] [::sim-pred/> :capacity 5]]

(sim-pred/apply-predicate-query pred-intent {:color :blue
                                              :capacity 6})
;;=> true
(sim-pred/apply-predicate-query pred-intent {:color :blue
                                              :capacity 3})
;;=> false
(sim-pred/apply-predicate-query pred-intent {:color :green
                                              :capacity 6})
;;=> false
(sim-pred/apply-predicate-query pred-intent {:color :green
                                              :capacity 1})
;;=> false
```
And needs all of their predicates to return true.

```clojure
(def or-pred-intent [::sim-pred/or [::sim-pred/equal? :color :blue] [::sim-pred/> :capacity 5]]

(sim-pred/apply-predicate-query or-pred-intent {:color :blue
                                              :capacity 6})
;;=> true
(sim-pred/apply-predicate-query or-pred-intent {:color :blue
                                              :capacity 3})
;;=> true
(sim-pred/apply-predicate-query or-pred-intent {:color :green
                                              :capacity 6})
;;=> true

(sim-pred/apply-predicate-query or-pred-intent {:color :green
                                              :capacity 1})
;;=> false
```
::sim-pred/or will require any of predicates to be true to return true


```clojure
(def not-pred-intent [::sim-pred/not pred-intent]

(sim-pred/apply-predicate-query or-pred-intent {:color :blue
                                              :capacity 6})
;;=> false
(sim-pred/apply-predicate-query or-pred-intent {:color :blue
                                              :capacity 3})
;;=> false
(sim-pred/apply-predicate-query or-pred-intent {:color :green
                                              :capacity 6})
;;=> false

(sim-pred/apply-predicate-query or-pred-intent {:color :green
                                              :capacity 1})
;;=> true
```
::sim-pred/not will reverse the value returned by the predicate inside of it


## Predicates advanced
### Adding custom-made predicate
```clojure 

(1) (defn is-nil? [path]
    (fn [v]
     (= (get v path) nil)))

(2) (def my-custom-pred-reg {::is-nil? {:pred-fn is-nil?}}) 

(3)  (sim-pred/apply-predicate-query 
    (merge sim-pred/predicates-lang-reg
           my-custom-pred-reg)
    [::is-nil? :color] 
    {:color ["hello" "world" "predicates"]})
;;=> false
```

(1) we declare a predicate function. All parameters of that function will be what is expected as vector language params.
It can accept any number of params, could be also no params.

The function is expected to return a function that as a parameter accepts a value that will be used for comparison which result should be:
true - when it matches the predicate 
false - when not
nil - if it's impossible to tell 

In this particular function, we expect that our query will look like this:
[::is-nil? :path]
which turned into a function will look into the value path and return true if it's nil.

(2) We define our custom registry. 
It needs to follow only two rules:
the name of the predicate is a keyword in the map
value is a map containing :pred-fn and the corresponding function definition

All other data in the map are optional, but it's a good practice to add :doc to explain it and :validation-fn which will help in detecting before evaluation if the vector language is valid 


(3) apply-predicate-query can accept additional param, which is a registry it uses to turn predicate query vectors to functions. 
Here as a first argument to apply-predicate-query, we merge the default predicates registry with our custom one. 
The default one does not have to be used, although all its keys are namespaced, so there should not be a clash with your registry 

