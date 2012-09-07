(ns ruuvi-ui.map
  (:require [ruuvi-ui.util :as util]
            [jayq.core :as jquery])
  )

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

(defn add-tracker-data [trackers-data]
  ;; Update tracker data to trackers-store. Overwrite existing
  ;; trackers.
  (let [trackers (get trackers-data "trackers")]
    (swap! trackers-store
           (fn [old-list]
             (reduce (fn [sum tracker]
                       (update-in sum [("id" tracker) :tracker]
                                  (fn [_] tracker)))
                     old-list
                     trackers))))
  )

(defn- sort-events [events]
  (sort-by #(get % "event_time") events))
  
(defn- merge-events [old-events new-events]
  (let [events (concat old-events new-events)
        sorted-events (sort-by #(get % "id") events)
        grouped-events (partition-by #(get % "id") sorted-events)
        deduped-events (map first grouped-events)]
    deduped-events))

(defn- add-tracker-event-data [tracker-id new-events]
  ;; merge new-events with old events and remove dupes
  (swap! trackers-store
         (fn [trackers]
           (let [trackers (update-in trackers [tracker-id :events]
                                     #(sort-events (merge-events % new-events)))             
                 trackers (update-in trackers [tracker-id :latest-event-time]
                                     (fn [time]
                                       (apply max (conj (map #(get % "event_time") new-events) time))) )
                 trackers (update-in trackers [tracker-id :latest-store-time]
                                     (fn [time]
                                       (apply max (conj (map #(get % "store_time") new-events) time))) )
                 ]
             trackers
             ))
         )
  )

(defn add-event-data [events-data]
  ;; group events by tracker_id
  (let [events (get events-data "events")
        grouped (group-by #(get % "tracker_id") events)]
    (doall
     (map #(add-tracker-event-data (key %) (val %)) grouped))
  ))
