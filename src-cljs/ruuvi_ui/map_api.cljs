(ns ruuvi-ui.map-api
  (:require [ruuvi-ui.util :as util]
            [ruuvi-ui.storage :as storage]
            )
  (:use [ruuvi-ui.data :only [state]]
        [clojure.string :only [split]]
        [jayq.core :only [$ replaceWith]]
        [jayq.util :only [clj->js]]
        [ruuvi-ui.log :only [debug info warn error]]
        )
  )

(declare start-locating)

(defn create-osm-tiles []
  (let [tile-url "http://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        tile-opts (js-obj "attribution" "Map data &copy; <a href='http://openstreetmap.org'>OpenStreetMap</a> contributors, <a href='http://creativecommons.org/licenses/by-sa/2.0/'>CC-BY-SA</a>"
                          "maxZoom" 18)
        tiles (new js/L.TileLayer tile-url tile-opts)]
    tiles))

(defn center-map [location & [zoom]]
  (let [zoom (or zoom 13)]
    (debug (str "Centering map on " (.-lat location) " " (.-lng location) " with zoom " zoom))
    (.setView (@state :map-view) location zoom)
    ))

(defn- update-self-location
  "Updates location of self marker."
  [new-location]
  (info "Set self location to" new-location)
  (swap! state #(update-in % [:self-location]
                           (fn [{:keys [old-location marker] :as old-value}]
                             (let [marker-options {:zIndexOffset 9000}
                                   marker (util/update-marker marker new-location (@state :map-view) marker-options)]
                               (merge old-value {:location new-location :marker marker}))
                             )))
  )

(defn- store-map-location [event]
  (let [map-view (:map-view @state)
        location (.getCenter map-view)
        zoom (.getZoom map-view)
        data {:zoom zoom
              :latitude (.-lat location) :longitude (.-lng location)
              :timestamp (.getTime (new js/Date))}]
    (storage/store :map-location data)))

(defn- fetch-map-location [timeout-seconds]
  (let [now (.getTime (new js/Date))
        value (storage/fetch :map-location)
        value-timestamp (:timestamp value)]
    (if (and value-timestamp
             (> (- now  (* timeout-seconds 1000)) value-timestamp))
      nil
      value)))

(defn- set-initial-location
  "Use stored location if it is newer than 1h, otherwise use current location."
  [default-location]
  (let [hour (* 60 60)
        {:keys [timestamp latitude longitude zoom]} (fetch-map-location hour)]
    (if latitude
      (center-map (new js/L.LatLng latitude longitude) zoom)
      (center-map default-location)
      )))

(defn- create-map [canvas-id start-location]
  (info "Create new map. Start location:" (.-lat start-location) (.-lng start-location) )
  (let [tiles (create-osm-tiles)
        new-map-view (new js/L.Map canvas-id)]

    (.addLayer new-map-view tiles)
    (.on new-map-view "locationfound" (fn [e]
                                        (let [location (.-latlng e)]
                                          (update-self-location location)
                                          )))
    (.on new-map-view "locationerror" (fn [e] (js/console.log "Location error" e)))
    (swap! state #(assoc % :map-view new-map-view))
    (set-initial-location start-location)
    ;; set up move events here so the do not mess up set-initial-location
    (.on new-map-view "zoomend" store-map-location)
    (.on new-map-view "moveend" store-map-location)
    (start-locating)
    new-map-view))

;; TODO check if actually needed (zoom buttons broke before)
(defn- reattach-controls
  "Remove and add controls back to map. Some controls break when redisplaying map."
  [map-view]
  (let [old-control (.-zoomControl map-view)]
    (.removeFrom old-control map-view)
    (.addTo old-control map-view)))

(defn- redisplay-map
  "Displays existing map in given location in DOM."
  [canvas-id map-view]
  (info "Redisplay existing map.")
  (let [map-container (.getContainer map-view)
        placeholder ($ (str "#" canvas-id))]
    (.replaceWith placeholder map-container)
    (reattach-controls map-view)
    ))

(defn open-map
  "Opens map at given canvas-id. If map doesn't exist it is created and centered on users current location or given start-location. If map already exists, its center point is not changed."
  [& [canvas-id start-location]]
  (let [existing-map-view (@state :map-view)]
    (if existing-map-view
      (redisplay-map canvas-id existing-map-view)
      (create-map canvas-id start-location)
      )))

(defn locate []
  (debug "Locating self")
  (when-let [self-location (:location (:self-location @state))]
    (center-map self-location 18)))

(defn start-locating []
  (debug "Starting continuous locating")
  (let [options {:timeout 10000
                 :maximumAge 10000
                 :enableHighAccuracy true
                 :watch true}]
    (.locate (@state :map-view) (clj->js options))))

(defn stop-locating []
  (.stopLocate (@state :map-view)))


