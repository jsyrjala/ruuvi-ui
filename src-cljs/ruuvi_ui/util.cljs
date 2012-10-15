(ns ruuvi-ui.util
  (:require [jayq.core :as jquery])
  (:use [jayq.util :only [clj->js]])
  )

(defn get-event-coordinate
  "Returns [latitude longitude]."
  [event]
  (let [lat (get-in event [:location :latitude])
        lng (get-in event [:location :longitude])]
    (when (and lat lng)
      [lat lng] )))

(defn get-event-lat-lng
  "Returns L.LatLng object if coordinates exist."
  [event]
  (let [[lat lng] (get-event-coordinate event)]
    (when (and lat lng)
      (new js/L.LatLng lat lng))))


(defn get-first-location [events]
  (get-event-lat-lng (last (filter get-event-coordinate events)))
  )

(defn update-marker
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
