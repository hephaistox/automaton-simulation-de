(ns automaton-simulation-de.control.computation.response
  "Computation response is consisting of status and scheduler response.
   Status informs about how the response is related to the query asked to computation.
   :success <- it is as expected
   :no-next <- it is not what was asked, but it is impossible to continue further in simulation (e.g. model-end, no future events...)
   :timeout <- it is not what was asked and it can be continued to search, but it hit a limit of time as it may be endless simulation or question could never be answered
   :internal-error <- The response may be corrupted as there was some problem with execution itself

    For diagram see ![computation](archi/control/computation_response.png)"
  (:require
   [automaton-simulation-de.control                    :as-alias sim-de-control]
   [automaton-simulation-de.simulation-engine.response :as sim-de-response]))

(def schema
  [:map {:closed true}
   [::sim-de-control/status [:enum :success :no-next :timeout :internal-error]]
   [::sim-de-control/response sim-de-response/schema]])

(defn build
  [status response]
  {::sim-de-control/status status
   ::sim-de-control/response response})
