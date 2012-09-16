(ns ruuvi-ui.api
  (:require [ruuvi-ui.util :as util]
            )
  (:use [jayq.util :only [clj->js]]
        [jayq.core :only [$ css inner val ajax]]
        [clojure.walk :only [keywordize-keys]]
        [ruuvi-ui.log :only [debug info warn error]])
  )

;;(def base-url "http://ruuvi-server.herokuapp.com/api/v1-dev/")
(def base-url "http://localhost:3000/api/v1-dev/")

(defn- log-request-error [e]
  (error (str "AJAX request failed" e)))

(defn- wrap-success-fn
  "Convert incoming JSON data to Clojure data structure and convert string keys to keywords."
  [func]
  (fn [data text-status jqxhr]
    (func (keywordize-keys (js->clj data)) text-status jqxhr)))
  
(defn- ajax-url-request [url success-fn error-fn]
  (let [request (.getJSON js/jQuery url (wrap-success-fn success-fn))]
    (.error request (or error-fn log-request-error)))
    )

(defn- ajax-param-request [url params success-fn error-fn]
  (let [request (.getJSON js/jQuery url (clj->js params)
                          (wrap-success-fn success-fn))]

    (.fail request (or error-fn log-request-error)))
  )

(defn- ajax-post-request [url params success-fn error-fn]
  (let [request (.ajax js/jQuery url (clj->js params))]
    (.done request wrap-success-fn success-fn)
    (.done request (or error-fn log-request-error))
  ))

(defn get-trackers [success-fn error-fn]
  (debug "Fetching trackers")
  (ajax-url-request (str base-url "trackers") success-fn error-fn))
            
(defn get-events [tracker-id since-timestamp success-fn error-fn]
  (debug (str "Fetching events for tracker" tracker-id " after " since-timestamp ))
  (let [results-since (if since-timestamp (str "storeTimeStart=" since-timestamp) "")]
    (ajax-url-request (str base-url "trackers/" tracker-id "/events?" results-since)
                      success-fn error-fn)))

(defn- parse-search-location-response [func]
  (fn [data text-status jqxhr]
    (let [parsed-data (into [] 
                            (map (fn [item]
                                   (let [value (select-keys item ["icon" "display_name"])
                                         lat (:lat item)
                                         lon (:lon item)
                                         value (merge value
                                                      {:location (new js/L.LatLng lat lon)})]
                                    value
                                    (keywordize-keys value)
                                    )) data))]
      (func parsed-data text-status jqxhr))))

(defn search-location
  [address success-fn & [error-fn]]
  (when (and address (not (empty? address)))
    (info "Locating address:" address)
    (let [url "http://nominatim.openstreetmap.org/search"]
      (ajax-param-request url {:q address :format :json} (parse-search-location-response success-fn) error-fn))))

(defn create-tracker [{:keys [name code shared-secret demo-password success-fn error-fn]}]
  (info "Creating a new tracker")
  (let [message {:tracker {:name name
                           :code code
                           :shared_secret code
                           }
                 }
        settings {:type "POST"
                  :data (js/JSON.stringify (clj->js message))
                  :processData false
                  :contentType "application/json"
                  :success success-fn
                  :error error-fn
                  }
        url (str base-url "trackers")]
    (ajax-post-request url settings success-fn error-fn)
  ))