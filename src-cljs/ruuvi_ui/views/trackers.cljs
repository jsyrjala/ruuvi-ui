(ns ruuvi-ui.views.trackers
  (:require [enfocus.core :as ef]
            [ruuvi-ui.util :as util]
            [ruuvi-ui.api :as api]
            [ruuvi-ui.view :as view]
            [ruuvi-ui.views.navigation :as navi]
            [ruuvi-ui.map :as map]
            )
  (:use [ruuvi-ui.log :only [debug info warn error]])
  (:require-macros [enfocus.macros :as em])
  )

(em/deftemplate trackers-template "templates/trackers-page.html" [])

(defmethod view/init-content :trackers []

  )

(defmethod view/content-template :trackers []
  (trackers-template))

