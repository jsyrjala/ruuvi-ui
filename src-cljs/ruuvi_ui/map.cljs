(ns ruuvi-ui.map
  (:require [ruuvi-ui.util :as util])
  (:use [jayq.core :only [$ replaceWith]]
        [jayq.util :only [clj->js]]
  ))

(declare locate)

;; TODO support several maps?
(def map-view (atom nil))

(def self-location (atom {}))

;; {tracker-id1 {:tracker <tracker-object>
;;               :latest-event-time
;;               :latest-store-time
;;               :events [event1 event2]
;;               :marker <marker-object>
;;               :path <path-object>}}
(def trackers-store (atom {}))
  
(defn create-osm-tiles []
  (let [tile-url "http://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        tile-opts (js-obj "attribution" "Map data &copy; <a href='http://openstreetmap.org'>OpenStreetMap</a> contributors, <a href='http://creativecommons.org/licenses/by-sa/2.0/'>CC-BY-SA</a>"
                   "maxZoom" 18)
        tiles (new js/L.TileLayer tile-url tile-opts)]
    tiles))

(defn center-map [location & [zoom]]
  (let [zoom (or zoom 13)]
    (util/log (str "Centering map on " (.-lat location) " " (.-lng location) " with zoom " zoom))
    (.setView @map-view location zoom)
  ))

(defn- update-self-location
  "Updates location of self marker."
  [new-location]
  (swap! self-location (fn [{:keys [old-location marker] :as old-value}]
                         (let [marker (or marker (let [m (new js/L.Marker new-location)]
                                                   (.addTo m @map-view)
                                                   m))]                     
                           (.setLatLng marker new-location)
                           (.update marker)
                           (merge old-value {:location new-location :marker marker}))
                         )))

(defn- create-map [canvas-id start-location]
  (util/log "Create new map. Start location:" (.-lat start-location) (.-lng start-location) )
  (let [tiles (create-osm-tiles)
        new-map-view (new js/L.Map canvas-id)]
    (.addLayer new-map-view tiles)
    (.on new-map-view "locationfound" (fn [e]
                                        (let [location (.-latlng e)]
                                          (center-map location 18)
                                          (update-self-location location)
                                          )))
    (.on new-map-view "locationerror" (fn [e] (js/console.log "Location error" e)))
    (reset! map-view new-map-view)
    (center-map start-location)
    (locate)
    new-map-view))

(defn- reattach-controls
  "Remove and add controls back to map. Some controls break when redisplaying map."
  [map-view]
  (let [old-control (.-zoomControl map-view)]
    (.removeFrom old-control map-view)
    (.addTo old-control map-view)))

(defn- redisplay-map
  "Displays existing map in given location in DOM."
  [canvas-id map-view]
  (util/log "Redisplay existing map.")
  (let [map-container (.getContainer map-view)
        placeholder ($ (str "#" canvas-id))]
    (.replaceWith placeholder map-container)
    (reattach-controls map-view)
  ))

(defn open-map
  "Opens map at given canvas-id. If map doesn't exist it is created and centered on users current location or given start-location. If map already exists, its center point is not changed."
  [& [canvas-id start-location]]
  (let [existing-map-view @map-view]
    (if existing-map-view
      (redisplay-map canvas-id existing-map-view)
      (create-map canvas-id start-location)
  )))

(defn locate []
  (util/log "Locating self")
  (let [options (js-obj "timeout" 2000
                        "maximumAge" 10000
                        "enableHighAccuracy" true)]
    (.locate @map-view options)
    ))

(defn add-tracker-data [trackers-data]
  ;; Update tracker data to trackers-store. Overwrite existing
  ;; trackers.
  (let [trackers (:trackers trackers-data)]
    (swap! trackers-store
           (fn [old-list]
             (reduce (fn [sum tracker]
                       (update-in sum [(:id tracker) :tracker]
                                  (fn [_] tracker)))
                     old-list
                     trackers))))
  )

(defn- sort-events [events]
  (sort-by :event_time events))
  
(defn- merge-events [old-events new-events]
  (let [events (concat old-events new-events)
        sorted-events (sort-by :id events)
        grouped-events (partition-by :id sorted-events)
        deduped-events (map first grouped-events)]
    deduped-events))

(defn- get-event-coordinate [event]
  (let [lat (get-in event [:location :latitude])
        lng (get-in event [:location :longitude])]
    (when (and lat lng)
      (new js/L.LatLng lat lng)
    )))

(defn- update-path [existing-path events]
  (let [coordinates (map get-event-coordinate events)
        coordinates (filter identity coordinates)]
    (if existing-path
      (.setLatLngs existing-path (clj->js coordinates))
      (let [path (new js/L.Polyline (clj->js coordinates))]
        (.addTo path @map-view)))
  ))

(defn- add-tracker-event-data [tracker-id new-events]
  ;; merge new-events with old events and remove dupes
  ;; TODO separate events to sessions
  (swap! trackers-store
         (fn [trackers]
           (let [trackers (update-in trackers [tracker-id :events]
                                     #(sort-events (merge-events % new-events)))             
                 trackers (update-in trackers [tracker-id :latest-event-time]
                                     (fn [time]
                                       (apply max (conj (map :event_time new-events) time))) )
                 trackers (update-in trackers [tracker-id :latest-store-time]
                                     (fn [time]
                                       (apply max (conj (map :store_time new-events) time))) )
                 events (get-in trackers [tracker-id :events])
                 trackers (update-in trackers [tracker-id :path]
                                     (fn [existing-path]
                                       (update-path existing-path events)) )
                 ]
             trackers
             ))
         )
  )

(defn add-event-data [events-data]
  ;; group events by tracker_id
  (let [events (:events events-data)
        grouped (group-by :tracker_id events)]
    (doall
     (map #(add-tracker-event-data (key %) (val %)) grouped))
  ))
