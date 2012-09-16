(ns ruuvi-ui.views.create_tracker
  (:require [enfocus.core :as ef]
            [ruuvi-ui.api :as api]
            [ruuvi-ui.view :as view]
            )
  (:require-macros [enfocus.macros :as em])
  (:use [ruuvi-ui.log :only [debug info warn error]])
  )

(em/deftemplate create-tracker-template "templates/create-tracker-page.html" [])

(defn- create-tracker [event]
  (info "Creating a new tracker")
  ;; returning false will not work here
  (.preventDefault event)
  (.stopPropagation event)
  )

(em/defaction init-components []
  [:#create-tracker-button] (em/listen :click create-tracker))

(defmethod view/init-content :create-tracker []
  (init-components))

(defmethod view/content-template :create-tracker []
  (create-tracker-template))

