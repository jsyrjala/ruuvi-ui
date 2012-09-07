(ns ruuvi-ui.api
  (:require [ruuvi-ui.util :as util])
  (:use [jayq.util :only [clj->js]])
  )

(def base-url "http://ruuvi-server.herokuapp.com/api/v1-dev/")

(defn- log-request-error [e]
  (util/log (str "AJAX request failed" e)))

(defn- wrap-success-fn
  "Convert incoming JSON data to Clojure data structure."
  [func]
  (fn [data text-status jqxhr]
    (func (js->clj data) text-status jqxhr)))
  
(defn- ajax-url-request [url success-fn error-fn]
  (let [request (.getJSON js/jQuery url (wrap-success-fn success-fn))]
    (.error request (or error-fn log-request-error)))
    )

(defn- ajax-param-request [url params success-fn error-fn]
  (let [request (.getJSON js/jQuery url (clj->js params)
                          (wrap-success-fn success-fn))]

    (.error request (or error-fn log-request-error)))
  )

(defn get-trackers [success-fn error-fn]
  (ajax-url-request (str base-url "trackers") success-fn error-fn))
            
(defn get-events [tracker-id since-timestamp success-fn error-fn]
  (let [results-since (if since-timestamp (str "storeTimeStart=" since-timestamp) "")]
    (ajax-url-request (str base-url "trackers/" tracker-id "/events?" results-since)
              success-fn error-fn)))

(defn- keyword-keys [m]
  (into {} (map (fn [[k v]] {(keyword k) v}) m)))

(defn- parse-search-location-response [func]
  (fn [data text-status jqxhr]
    (let [parsed-data (into [] 
                            (map (fn [item]
                                   (let [value (select-keys item ["icon" "display_name"])
                                         lat (item "lat")
                                         lon (item "lon")
                                         value (merge value
                                                      {:location (new js/L.LatLng lat lon)})]
                                    value
                                    (keyword-keys value)
                                    )) data))]
      (func parsed-data text-status jqxhr))))

(defn search-location
  [address success-fn & [error-fn]]
  (let [url "http://nominatim.openstreetmap.org/search"]
    (ajax-param-request url {:q address :format :json} (parse-search-location-response success-fn) error-fn)))
    
