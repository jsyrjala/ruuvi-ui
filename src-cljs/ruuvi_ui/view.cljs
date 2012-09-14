(ns ruuvi-ui.view
  (:require [enfocus.core :as ef]
            [ruuvi-ui.util :as util]
            [ruuvi-ui.api :as api]
            [ruuvi-ui.map :as map]
            )
  (:use [jayq.core :only [$ css inner val]]
        [ruuvi-ui.log :only [debug info warn error]])
  (:require-macros [enfocus.macros :as em])
  )

(defn- supported-page [page]
  (or (some #{:index :map :trackers :create-tracker :help} [page]) :error))

(defn- page-to-selector [page]
  (case page
    :create-tracker ".page-link-trackers"
    (let [page-selector (str ".page-link-" (name (supported-page page)))]
      page-selector)))

(em/deftemplate navigation-template "templates/navigation.html" [active-page]
  [(page-to-selector active-page)] (em/add-class "active"))

(em/deftemplate index-template "templates/index-page.html" [])
(em/deftemplate map-template "templates/map-page.html" [])
(em/deftemplate trackers-template "templates/trackers-page.html" [])
(em/deftemplate help-template "templates/help-page.html" [])
(em/deftemplate create-tracker-template "templates/create-tracker-page.html" [])
(em/deftemplate error-template "templates/error-page.html" [])

(em/deftemplate location-search-template "templates/location-search.html" [])

;;;;;;

(defn- start-map-view [canvas-id]
  (let [start-location (new js/L.LatLng 60.168564, 24.941111)]
    (map/open-map canvas-id start-location)
  ))

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


;;;;;;

(defn- content-template [page]
  (case page
    :index (index-template)
    :map (map-template)
    :trackers (trackers-template)
    :create-tracker (create-tracker-template)
    :help (help-template)
    (do
      (warn (str "Page " page " not found"))
      (error-template))))

(em/defaction display-navigation-template [page]
  ["#top-navigation"] (em/content (navigation-template page)))

(declare init-navigation)
(declare start-map-view)
(declare display-page)

(defn display-navigation [page]
  (display-navigation-template page)
  (init-navigation page))

(em/defaction init-navigation [page]
  ["a[href='#']"] (em/listen :click #(display-page :index))
  ["a[href='#map']"] (em/listen :click #(display-page :map))
  ["a[href='#trackers']"] (em/listen :click #(display-page :trackers))
  ["a[href='#help']"] (em/listen :click #(display-page :help)))

(defmulti init-content identity)

(defn- display-location [locations]
  (when (not (empty? locations))
    (if-let [loc (first locations)]
      (map/center-map (:location loc))
  )))

(defn- query-location []
  (let [input ($ :#location-search_input)]
    (api/search-location (val input) display-location))
  false)

(em/defaction init-location-search []
  [:#location-search] (em/content (location-search-template))
  [:#location-search_submit] (em/listen :click query-location))

(defmethod init-content :map []
  (let [start-location (new js/L.LatLng 60.168564, 24.941111)
        tiles (map/create-osm-tiles)]
    (start-map-view "map-canvas")
    (start-buttons)
    (init-location-search)
  ))


(em/defaction init-create-tracker []
  ["a[href='#create-tracker']"] (em/listen :click #(display-page :create-tracker))
  )
(defmethod init-content :trackers []
  (init-create-tracker)
  )

(defmethod init-content :default []
  ;; nothing here
  )

(em/defaction display-content-template [page]
  ["#content"] (em/content (content-template page)))

(defn display-content [page]
  (display-content-template page)
  (init-content page))

(defn display-page [page]
  (info "Displaying page" page)
  (display-content page)
  (display-navigation page)
  )
