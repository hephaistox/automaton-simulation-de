(ns automaton-simulation-de.simulation-engine.request-test
  (:require
   #?(:clj [clojure.test :refer [deftest is testing]]
      :cljs [cljs.test :refer [deftest is testing] :include-macros true])
   [automaton-core.adapters.schema                    :as core-schema]
   [automaton-simulation-de.simulation-engine         :as-alias sim-engine]
   [automaton-simulation-de.simulation-engine.request :as sut]))

(deftest schema-test
  (testing "Test the schema"
    (is (= nil (core-schema/validate-humanize sut/schema)))))

(deftest add-stopping-cause-test
  (is
   (=
    #:automaton-simulation-de.simulation-engine{:request true
                                                :stopping-causes
                                                [#:automaton-simulation-de.simulation-engine{:stopping-cause
                                                                                             true}]}
    (sut/add-stopping-cause
     #:automaton-simulation-de.simulation-engine{:request true}
     #:automaton-simulation-de.simulation-engine{:stopping-cause true}))
   "Adding a `stopping-cause` returns the request.")
  (is (= {:request true} (sut/add-stopping-cause {:request true} nil))
      "Adding a `stopping-cause` returns the request."))
