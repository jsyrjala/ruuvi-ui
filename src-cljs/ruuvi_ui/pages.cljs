(ns ruuvi-ui.pages
  (:require [enfocus.core :as ef]
            [ruuvi-ui.view :as view]
            [ruuvi-ui.views.navigation :as navi]

            [ruuvi-ui.views.map :as map]
            [ruuvi-ui.views.trackers :as trackers]
            [ruuvi-ui.views.create-tracker :as create-tracker]
            [ruuvi-ui.views.debug :as debug]
            )
  (:use [ruuvi-ui.log :only [debug info warn error]])
  (:require-macros [enfocus.macros :as em])
  )

(defn- supported-page [page]
  (or (some #{:index :map :trackers :create-tracker :help :debug} [page]) :error))

(defn- page-to-selector [page]
  (case page
    :create-tracker ".page-link-trackers"
    (let [page-selector (str ".page-link-" (name (supported-page page)))]
      page-selector)))

(em/deftemplate index-template "templates/index-page.html" [])

(defmethod view/content-template :index []
  (index-template))

(em/deftemplate help-template "templates/help-page.html" [])

(defmethod view/content-template :help []
  (help-template))

(defn display-page [page]
  (navi/display-page page))
