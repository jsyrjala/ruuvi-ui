(ns ruuvi-ui.views.map
  (:require [enfocus.core :as ef]
            [ruuvi-ui.api :as api]
            [ruuvi-ui.view :as view]
            [ruuvi-ui.map-api :as map-api]
            [ruuvi-ui.data :as data]
            [ruuvi-ui.storage :as storage]
            )
  (:use [jayq.core :only [$ val]]
        [ruuvi-ui.log :only [debug info warn error]])
  (:require-macros [enfocus.macros :as em])
  )

(em/deftemplate map-template "templates/map-page.html" [])
(em/deftemplate location-search-template "templates/location-search.html" [])

(defn- query-func []
  (let [tracker-ids (storage/fetch :selected-trackers)]
    (doseq [tracker-id tracker-ids]
      (let [trackers (:trackers (deref data/state))
            store-time (get-in trackers [tracker-id :latest-store-time])]
        (api/get-events tracker-id store-time data/add-event-data error)
    ))))

(em/defaction start-buttons []
  ["#locate-me"] (em/listen :click #(map-api/locate))
  ["#query"] (em/listen :click query-func))

(defn- start-map-view [canvas-id]
  (let [start-location (new js/L.LatLng 60.168564, 24.941111)]
    (map-api/open-map canvas-id start-location)
  ))

(defn- display-location [locations]
  (when (not (empty? locations))
    (if-let [loc (first locations)]
      (map-api/center-map (:location loc))
  )))

(defn- query-location [event]
  (.preventDefault event)
  (.stopPropagation event)
  (let [input ($ :#location-search_input)]
    (api/search-location (val input) display-location))
  )

(em/defaction init-location-search []
  [:#location-search] (em/content (location-search-template))
  [:#location-search_form] (em/listen :submit query-location))

(defmethod view/init-content :map []
  (let [start-location (new js/L.LatLng 60.168564, 24.941111)
        tiles (map-api/create-osm-tiles)]
    (start-map-view "map-canvas")
    (start-buttons)
    (init-location-search)
  ))

(defmethod view/content-template :map []
  (map-template))
