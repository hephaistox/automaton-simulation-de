(ns automaton-simulation-de.control.computation
  "Computation is responsible for execution/storage of scheduler so that it can respond to questions about simulation.
   This namespace consists of protocol that defines core questions to answer and use-case functions of that protocol

  For diagram see ![computation](archi/control/computation.png)")

(defprotocol Computation
  "Returns information about specific simulation that are useful for rendering"
  (scheduler-response [this]
                      [this stopping-criterias]
                      [this stopping-criterias iteration]
   "Return scheduler response when any of stopping-criteria is matched from `stopping-criterias` collection.
     Empty `stopping-criterias` means that it will stop at first found.
     `iteration` is additional information to tell from which snapshot iteration to start looking for stopping-criterias")
  (stopping-criterias [this]
   "Returns all stopping-criteria that may occur in this simulation"))

(defn iteration-n
  "Returns `computation-scheduler-response` containing snapshot with iteration `n`"
  [computation n]
  (scheduler-response computation
                      [[:iteration-nth {:n n}]]
                      n))

(defn not-next?
  "Is next iteration possible?"
  [stopping-causes]
  (some
   #(or (false?
         (get-in % [:stopping-criteria :stopping-definition :next-possible?]))
        (true? (get-in % [:stopping-criteria :model-end?])))
   stopping-causes))

(defn stopping-criteria-model-end?
  "Returns true if simulation has model-end criterias defined"
  [computation]
  (let [registry (stopping-criterias computation)] (some :model-end? registry)))

(defn model-end-iteration
  "Returns scheduler response when model-end? stopping-criteria is reached"
  ([computation]
   (let [criterias (stopping-criterias computation)
         model-end-stopping-criterias (vec (filter #(:model-end? %) criterias))
         iteration
         (scheduler-response computation model-end-stopping-criterias 0)]
     iteration)))
