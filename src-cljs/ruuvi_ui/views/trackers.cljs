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

(em/defsnippet tracker-list "templates/components.html" ["#tracker-list > tbody > *"]
  [tracker]
  [".tracker-name"] (em/content (:name tracker))
  [".tracker-code"] (em/content (:tracker_code tracker))
  )

(em/deftemplate trackers-template "templates/trackers-page.html" [trackers]
  ["#tracker-list"] (em/content (map #(tracker-list %) trackers)))


(defmethod view/init-content :trackers []

  )

(defmethod view/content-template :trackers []
  (let [trackers [{:name "Tracker 1" :tracker_code "abc"}
                  {:name "Tracker 2" :tracker_code "foob"}]]
    (trackers-template trackers)))

