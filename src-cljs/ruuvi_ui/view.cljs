(ns ruuvi-ui.view
  (:require [enfocus.core :as ef]
            [ruuvi-ui.util :as util]
            [ruuvi-ui.api :as api]
            [ruuvi-ui.map :as map]
            )
  (:use [jayq.core :only [$ css inner val]])
  (:require-macros [enfocus.macros :as em]                   )
  )

(defn- supported-page [page]
  (or (some #{:index :map :trackers} [page]) :error))

(defn- page-to-selector [page]
  (let [page-selector (str ".page-link-" (name (supported-page page)))]
    page-selector
    ))

(em/deftemplate navigation-template "templates/navigation.html" [active-page]
  [(page-to-selector active-page)] (em/add-class "active"))

(em/deftemplate index-template "templates/index-page.html" [])
(em/deftemplate map-template "templates/map-page.html" [])
(em/deftemplate trackers-template "templates/trackers-page.html" [])
(em/deftemplate help-template "templates/help-page.html" [])
(em/deftemplate error-template "templates/error-page.html" [])

(em/deftemplate location-search-template "templates/location-search.html" [])

;;;;;;

(defn- start-map-view [canvas-id]
  (let [start-location (new js/L.LatLng 60.168564, 24.941111)
        tiles (map/create-osm-tiles)]
    (map/create-map canvas-id tiles start-location)
  ))

(defn- show-events [data]
  (util/log "show-events" data)
  )

(defn f []
  ;;  (api/get-events 4 nil (fn [data] (show-events (js->clj data))) util/log)
  (api/get-trackers map/add-tracker-data util/log)
  )

(em/defaction start-buttons []
  ["#locate-me"] (em/listen :click #(map/locate))
  ["#query"] (em/listen :click f))


;;;;;;

(defn- content-template [page]
  (case page
    :index (index-template)
    :map (map-template)
    :trackers (trackers-template)
    :help (help-template)
    (error-template)))

(em/defaction load-navigation-template [page]
  ["#top-navigation"] (em/content (navigation-template page)))

(declare init-navigation)
(declare start-map-view)
(declare load-page)

(defn load-navigation [page]
  (load-navigation-template page)
  (init-navigation page))

(em/defaction init-navigation [page]
  ["a[href='#']"] (em/listen :click #(load-page :index))
  ["a[href='#map']"] (em/listen :click #(load-page :map))
  ["a[href='#trackers']"] (em/listen :click #(load-page :trackers))
  ["a[href='#help']"] (em/listen :click #(load-page :help)))


(defmulti init-content identity)

(defn- display-location [locations]
  (when (not (empty? locations))
    (if-let [loc (first locations)]
      (map/center-map (:location loc))
  )))

(defn- query-location []
  (let [input ($ :#location-search_input)]
    (api/search-location (val input) display-location)
    )
  false)

(em/defaction init-location-search []
  [:#location-search] (em/content (location-search-template))
  [:#location-search_submit] (em/listen :click query-location)
  )

(defmethod init-content :map []
  (let [start-location (new js/L.LatLng 60.168564, 24.941111)
        tiles (map/create-osm-tiles)]
    (start-map-view "map-canvas")
    (start-buttons)
    (init-location-search)
    (map/locate)
  ))

(defmethod init-content :default []
  ;; nothing here
  )

(em/defaction load-content-template [page]
  ["#content"] (em/content (content-template page)))

(defn load-content [page]
  (load-content-template page)
  (init-content page)
  )

(defn load-page [page]
  (load-navigation page)
  (load-content page))






