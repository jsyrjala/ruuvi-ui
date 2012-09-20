(ns ruuvi-ui.views.map
  (:require [enfocus.core :as ef]
            [ruuvi-ui.api :as api]
            [ruuvi-ui.view :as view]
            [ruuvi-ui.map :as map]
            )
  (:use [jayq.core :only [$ val]]
        [ruuvi-ui.log :only [debug info warn error]])
  (:require-macros [enfocus.macros :as em])
  )

(em/deftemplate map-template "templates/map-page.html" [])
(em/deftemplate location-search-template "templates/location-search.html" [])

(defn- show-events [data]
  (info "show-events" data)
  )

(defn- query-func []
  ;;  (api/get-events 4 nil (fn [data] (show-events (js->clj data))) error)
  ;; (api/get-trackers map/add-tracker-data error)
  (let [tracker-id "4"
        trackers (deref map/trackers-store)
        store-time (get-in trackers [tracker-id :latest-store-time])]
    (api/get-events tracker-id store-time map/add-event-data error)
  ))

(em/defaction start-buttons []
  ["#locate-me"] (em/listen :click #(map/locate))
  ["#query"] (em/listen :click query-func))


(defn- start-map-view [canvas-id]
  (let [start-location (new js/L.LatLng 60.168564, 24.941111)]
    (map/open-map canvas-id start-location)
  ))

(defn- display-location [locations]
  (when (not (empty? locations))
    (if-let [loc (first locations)]
      (map/center-map (:location loc))
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
        tiles (map/create-osm-tiles)]
    (start-map-view "map-canvas")
    (start-buttons)
    (init-location-search)
  ))

(defmethod view/content-template :map []
  (map-template))
