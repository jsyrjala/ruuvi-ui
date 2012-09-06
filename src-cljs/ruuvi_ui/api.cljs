(ns ruuvi-ui.api
  (:require [ruuvi-ui.util :as util])
  (:use [jayq.core :only [$]])
  )

(def base-url "http://ruuvi-server.herokuapp.com/api/v1-dev/")

(defn- log-request-error [e]
  (util/log (str "AJAX request failed" e)))

(defn get-trackers [success-fn error-fn]
  (.getJSON js/jQuery (str base-url "trackers") success-fn error-fn))
            
(defn get-events [tracker-id since-timestamp success-fn error-fn]
  (let [results-since (if since-timestamp (str "storeTimeStart=" since-timestamp) "")]
    (.getJSON js/jQuery (str base-url "trackers/" tracker-id "/events?" results-since)
              success-fn error-fn)))

