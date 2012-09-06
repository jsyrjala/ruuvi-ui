(ns ruuvi-ui.view
  (:require [enfocus.core :as ef]
            [ruuvi-ui.util :as util]
            [ruuvi-ui.api :as api]
            [ruuvi-ui.map :as map]
            )
  (:require-macros [enfocus.macros :as em]
                   )
  )
(defn- supported-page [page]
  (or (some #{:index :map :trackers} page) :error))

(defn- page-to-selector [page]
  (let [page-selector (str ".page-link-" (name (supported-page page)))
        ]
    page-selector
  ))

(em/deftemplate navigation-template "templates/navigation.html" [active-page]
  [(page-to-selector active-page)] (em/add-class "active"))

(em/deftemplate index-template "templates/index-page.html" [])
(em/deftemplate map-template "templates/map-page.html" [])
(em/deftemplate trackers-template "templates/trackers-page.html" [])
(em/deftemplate help-template "templates/help-page.html" [])
(em/deftemplate error-template "templates/error-page.html" [])

(defn- content-template [page]
  (case page
    :index (index-template)
    :map (map-template)
    :trackers (trackers-template)
    :help (help-template)
    (error-template)))


(em/defaction load-navigation-template [page]
  ["#top-navigation"] (em/content (navigation-template page)))

(em/defaction init-navigation [page]
  ["a[href='#']"] (em/listen :click #(load-content :index))
  ["a[href='#map']"] (em/listen :click #(load-content :map))
  ["a[href='#trackers']"] (em/listen :click #(load-content :trackers))
  ["a[href='#help']"] (em/listen :click #(load-content :help)))

(defn load-navigation [page]
  (load-navigation-template page)
  (init-navigation page))

(em/defaction load-content-template [page]
  ["#content"] (em/content (content-template page)))

(defmulti init-content identity)

(defn- start-map-view [canvas-id]
  (let [start-location (new js/L.LatLng 60.168564, 24.941111)
        tiles (map/create-osm-tiles)]
    (map/create-map canvas-id tiles start-location)
  ))

(defn show-events [data]
  (util/log data)
  )

(defn f []
  (util/log "JEE")
  (api/get-events 4 nil (fn [data] (show-events (js->clj data))) util/log)
  )

(em/defaction start-buttons []
  ["#locate-me"] (em/listen :click #(map/locate))
  ["#query"] (em/listen :click f))

(defmethod init-content :map []
  (let [start-location (new js/L.LatLng 60.168564, 24.941111)
        tiles (map/create-osm-tiles)]
    (start-map-view "map-canvas")
    (start-buttons)
    (map/locate)
  ))

(defmethod init-content :default []
  ;; nothing here
  )

(defn load-content [page]
  (load-content-template page)
  (init-content page)
  )

