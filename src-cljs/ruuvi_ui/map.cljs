(ns ruuvi-ui.map
  (:require [ruuvi-ui.util :as util])
  (:use [clojure.string :only [split]]
        [jayq.core :only [$ replaceWith]]
        [jayq.util :only [clj->js]]
        [ruuvi-ui.log :only [debug info warn error]]
        )
  )

(declare locate)

;; TODO support several maps?
(def map-view (atom nil))

(def self-location (atom {}))

;; {tracker-id1 {:tracker <tracker-object>
;;               :latest-event-time timestamp
;;               :latest-store-time timestamp
;;               :marker <marker-object>
;;               session-id1 {:events [event1 event2]
;;                            :path <path-object>}
;; }
(def trackers-store (atom {}))

(defn create-osm-tiles []
  (let [tile-url "http://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        tile-opts (js-obj "attribution" "Map data &copy; <a href='http://openstreetmap.org'>OpenStreetMap</a> contributors, <a href='http://creativecommons.org/licenses/by-sa/2.0/'>CC-BY-SA</a>"
                          "maxZoom" 18)
        tiles (new js/L.TileLayer tile-url tile-opts)]
    tiles))

(defn center-map [location & [zoom]]
  (let [zoom (or zoom 13)]
    (debug (str "Centering map on " (.-lat location) " " (.-lng location) " with zoom " zoom))
    (.setView @map-view location zoom)
    ))

(defn- update-marker
  "Updates existing marker or creates a new one."
  [marker new-location map & [options]]
  (if new-location
    (let [marker (or marker (let [new-marker (new js/L.Marker new-location (clj->js options))]
                              (.addTo new-marker map)
                              new-marker))]                     
      (.setLatLng marker new-location)
      (.update marker)
      marker)
    marker))

(defn- update-self-location
  "Updates location of self marker."
  [new-location]
  (swap! self-location (fn [{:keys [old-location marker] :as old-value}]
                         (let [marker-options {:zIndexOffset 9000}
                               marker (update-marker marker new-location @map-view marker-options)]
                           (merge old-value {:location new-location :marker marker}))
                         )))

(defn- create-map [canvas-id start-location]
  (info "Create new map. Start location:" (.-lat start-location) (.-lng start-location) )
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
  (info "Redisplay existing map.")
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
  (info "Locating self")
  (let [options (js-obj "timeout" 2000
                        "maximumAge" 10000
                        "enableHighAccuracy" true)]
    (.locate @map-view options)))

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

;; TODO this is slow, avoid!
(defn- sort-events [events]
  (sort-by :event_time events))

(defn- merge-events [old-events new-events]
  ;; OPTIMIZE assume that both event list are in order
  ;; concat if new-events is later than old events
  ;; (handle equal events on overlap)
  ;; otherwise do below merge/dedupe/sort
  (let [events (concat old-events new-events)
        sorted-events (sort-by :id events)
        grouped-events (partition-by :id sorted-events)
        deduped-events (map first grouped-events)]
    deduped-events))

(defn- get-event-coordinate
  "Returns [latitude longitude]."
  [event]
  (let [lat (get-in event [:location :latitude])
        lng (get-in event [:location :longitude])]
    (when (and lat lng)
      [lat lng] )))

(defn- get-event-lat-lng
  "Returns L.LatLng object if coordinates exist."
  [event]
  (let [[lat lng] (get-event-coordinate event)]
    (when (and lat lng)
      (new js/L.LatLng lat lng))))

(defn- update-path [existing-path events]
  (let [coordinates (map get-event-lat-lng events)
        coordinates (filter identity coordinates)]
    (if existing-path
      (let []
        ;; TODO optimize (recreating whole path is slow?)
        ;; if event_time for earliest new event is later or equal that
        ;; latest. 
        ;; old event => add to path, otherwise recreate whole path
        ;; Note that those events may also be identicals ->
        ;; must compare non identical events here.
        (.setLatLngs existing-path (clj->js coordinates)))
      (let [path (new js/L.Polyline (clj->js coordinates))]
        (.addTo path @map-view))) ))

(defn- get-first-location [events]
  (get-event-lat-lng (last (filter get-event-coordinate events)))
  )

(defn- add-tracker-event-data [tracker-id session-id new-events]
  ;; merge new-events with old events and remove dupes
  (swap! trackers-store
         (fn [trackers]
           (let [;; update events per tracker and session basis
                 trackers (update-in trackers [tracker-id session-id :events]
                                     #(sort-events (merge-events % new-events)))

                 ;; update paths per tracker and session basis
                 events (get-in trackers [tracker-id session-id :events])
                 trackers (update-in trackers [tracker-id session-id :path]
                                     (fn [existing-path]
                                       (update-path existing-path events)) )

                 ;; update current location marker per tracker
                 trackers (update-in trackers [tracker-id :marker]
                                     (fn [existing-marker]
                                       (update-marker existing-marker
                                                      (get-first-location events)
                                                      @map-view {})))
                                        
                 ;; update latest event timestamps per tracker basic
                 trackers (update-in trackers [tracker-id :latest-event-time]
                                     (fn [time]
                                       (apply max (conj (map :event_time new-events) time))) )
                 trackers (update-in trackers [tracker-id :latest-store-time]
                                     (fn [time]
                                       (apply max (conj (map :store_time new-events) time))) )
                 ]
             trackers
             ))
         )
  )

(defn- tracker-session-grouping [event]
  (let [tracker-id (:tracker_id event)
        session-id (:event_session_id event)]
    ;; TODO group-by doesn't work if [tracker-id event-id] is returned. it
    ;; returns undefined
    (str tracker-id "/" session-id)))

(defn- tracker-session-degrouping [grouping]
  (let [parts (split grouping "/")
        tracker-id (parts 0)
        session-id (parts 1)]
    [(or tracker-id nil) (or session-id nil)] ))

(defn add-event-data [events-data]
  ;; group events by tracker_id and event_session_id
  (let [events (:events events-data)
        grouped (group-by tracker-session-grouping events)]
    (doall
     (map (fn [g]
            (let [[tracker-id session-id] (tracker-session-degrouping (key g))]
              (add-tracker-event-data tracker-id session-id (val g)))) grouped)) ))

