(ns ruuvi-ui.map
  (:require [jayq.core :as jquery])
  )

(def map-view (atom nil))

(def self-marker (atom (new js/L.Marker (new js/L.LatLng 0.0 0.0))))

(defn ^:extern create-osm-tiles []
  (let [tile-url "http://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        tile-opts (js-obj "attribution" "Map data &copy; <a href='http://openstreetmap.org'>OpenStreetMap</a> contributors, <a href='http://creativecommons.org/licenses/by-sa/2.0/'>CC-BY-SA</a>"
                   "maxZoom" 18)
        tiles (new js/L.TileLayer tile-url tile-opts)]
    tiles))

(defn set-map-location! [location & [zoom marker]]
  (.setView @map-view location (or zoom 13))
  (when marker
    (.setOpacity marker 1)
    (.setLatLng marker location)
    (.update marker)
  ))

(defn create-map [canvasId tiles start-location]
  (let [new-map-view (new js/L.Map canvasId)
        ]
    (.addLayer new-map-view tiles)
    (.on new-map-view "locationfound" (fn [e]
                                        (set-map-location! (.-latlng e) 18 @self-marker)
                                        ))
    (.on new-map-view "locationerror" (fn [e] (js/console "Location error" e)))
    (reset! map-view new-map-view)
    (set-map-location! start-location)
    (.setOpacity @self-marker 0)
    (.addTo @self-marker new-map-view)
    new-map-view))

(defn ^:extern locate []
  (let [options (js-obj "timeout" 2000
                        "maximumAge" 10000
                        "enableHighAccuracy" true)]
    (.locate @map-view options)
    ))
