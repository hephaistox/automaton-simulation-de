(ns automaton-simulation-de.demo.queuing.state)

(def queueing-schema
  [:map [:input-stock [:vector]] [:parts [:map [:counter]]]
   [:servers [:map [:nb :integer]]]])

(def state {:input-stock [], :parts [:counter 10], :servers {:nb 4}})

(defn next-parts
  [{:keys [parts], :as state}]
  (let [next-part (inc (get parts :counter))]
    (assoc-in state [:parts :counter] next-part)))
