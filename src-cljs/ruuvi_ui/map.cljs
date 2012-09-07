(ns ruuvi-ui.map
  (:require [jayq.core :as jquery])
  )

(def map-view (atom nil))

(def self-location (atom {}))

;;
(def trackers (atom {}))
  
(defn create-osm-tiles []
  (let [tile-url "http://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        tile-opts (js-obj "attribution" "Map data &copy; <a href='http://openstreetmap.org'>OpenStreetMap</a> contributors, <a href='http://creativecommons.org/licenses/by-sa/2.0/'>CC-BY-SA</a>"
                   "maxZoom" 18)
        tiles (new js/L.TileLayer tile-url tile-opts)]
    tiles))

(defn center-map [location & [zoom]]
  (.setView @map-view location (or zoom 13))
  )

(defn- update-self-location [new-location]
  (swap! self-location (fn [{:keys [old-location marker] :as old-value}]
                         (let [marker (or marker (let [m (new js/L.Marker new-location)]
                                                   m))]
                           (.addTo marker @map-view)
                           (.setLatLng marker new-location)
                           (.update marker)
                           (merge old-value {:location new-location :marker marker}))
                         ))
  )

(defn set-map-location! [location & [zoom]]
  (center-map location zoom))

(defn create-map [canvasId tiles start-location]
  (let [new-map-view (new js/L.Map canvasId)
        ]
    (.addLayer new-map-view tiles)
    (.on new-map-view "locationfound" (fn [e]
                                        (let [location (.-latlng e)]
                                          (set-map-location! location 18)
                                          (update-self-location location)

                                          )))
    (.on new-map-view "locationerror" (fn [e] (js/console.log "Location error" e)))
    (reset! map-view new-map-view)
    (set-map-location! start-location)
    new-map-view))

(defn locate []
  (let [options (js-obj "timeout" 2000
                        "maximumAge" 10000
                        "enableHighAccuracy" true)]
    (.locate @map-view options)
    ))
