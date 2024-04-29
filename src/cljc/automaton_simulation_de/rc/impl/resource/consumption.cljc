(ns automaton-simulation-de.rc.impl.resource.consumption
  "Resource consumption is what is happening when a resource is available and an entity seizing it.

  A consumption has two steps, a consume and a free of the resource.
  The resource updates a `::sim-de-rc/currently-consuming` list events that are consuming resources"
  (:require
   [automaton-core.utils.uuid-gen :as uuid-gen]))

(defn consume
  "Consume `consumed-quantity` number of `resource`, store this consuming informations in the `currently-consuming` attribute of the resource.

  Returns a pair with:
     * the `consumption-uuid`
     * `resource` with `currently-consuming` updated with an entry under `consumption-uuid` entry with a map consisting in the two following entries:
        * `seizing-event` the event that has triggered a seizing.
        * `consumed-quantity` number consumed."
  [resource consumed-quantity seizing-event]
  (if (some? seizing-event)
    (let [consumption-uuid (uuid-gen/time-based-uuid)]
      [consumption-uuid
       (update resource
               :automaton-simulation-de.rc/currently-consuming
               assoc
               consumption-uuid
               #:automaton-simulation-de.rc{:seizing-event seizing-event
                                            :consumed-quantity
                                            consumed-quantity})])
    [nil resource]))

(defn free
  "Remove the seizing informations matching `consumption-uuid` for that resource.
  Returns the updated resource without that `consumption` anymore."
  [resource consumption-uuid]
  (update resource
          :automaton-simulation-de.rc/currently-consuming
          (fn [currently-consuming]
            (if (nil? currently-consuming)
              {}
              (dissoc currently-consuming consumption-uuid)))))
