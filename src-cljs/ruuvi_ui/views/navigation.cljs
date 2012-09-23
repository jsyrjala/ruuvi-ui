(ns ruuvi-ui.views.navigation
  (:require [enfocus.core :as ef]
            [ruuvi-ui.util :as util]
            [ruuvi-ui.api :as api]
            [ruuvi-ui.view :as view]
            )
  (:use [jayq.core :only [$ css inner val]]
        [ruuvi-ui.log :only [debug info warn error]])
  (:require-macros [enfocus.macros :as em])
  )

(defn- supported-page [page]
  (or (some #{:index :map :trackers :create-tracker :help :debug} [page]) :error))

(defn- page-to-selector [page]
  (case page
    :create-tracker ".page-link-trackers"
    (let [page-selector (str ".page-link-" (name (supported-page page)))]
      page-selector)))

(em/deftemplate navigation-template "templates/navigation.html" [active-page]
  [(page-to-selector active-page)] (em/add-class "active"))

(em/defaction display-navigation-template [page]
  ["#top-navigation"] (em/content (navigation-template page)))

(declare init-navigation)

(defn display-navigation [page]
  (display-navigation-template page)
  (init-navigation page))

(defn display-page [page]
  (info "Displaying page" page)
  (view/display-content page)
  (display-navigation page))

(em/defaction init-navigation [page]
  ["a[href='#index']"] (em/listen :click #(display-page :index))
  ["a[href='#map']"] (em/listen :click #(display-page :map))
  ["a[href='#trackers']"] (em/listen :click #(display-page :trackers))
  ["a[href='#create-tracker']"] (em/listen :click #(display-page :create-tracker))
  ["a[href='#help']"] (em/listen :click #(display-page :help))
  ["a[href='#debug']"] (em/listen :click #(display-page :debug))
  )

