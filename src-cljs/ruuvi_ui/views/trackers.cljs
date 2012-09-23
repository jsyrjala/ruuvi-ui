(ns ruuvi-ui.views.trackers
  (:require [enfocus.core :as ef]
            [ruuvi-ui.util :as util]
            [ruuvi-ui.api :as api]
            [ruuvi-ui.view :as view]
            [ruuvi-ui.views.navigation :as navi]
            [ruuvi-ui.map-api :as map-api]
            )
  (:use [ruuvi-ui.log :only [debug info warn error]])
  (:require-macros [enfocus.macros :as em])
  )
;; - list of trackers is hold in an atom
;; - trackers from atom is shown when page is displayed
;; - after page display tracker refresh is restarted (AJAX)

;; TODO
;; - store everything into single atom
;; - store tracker list to html5 local storage (if available)
;; - refresh trackers periodically
;; - allow selecting trackers as favorites
;;   - store favorite trackers to html5 local storage
;; - display latest activity for each tracker (2 min ago etc)
;; - localization for different languages
(def trackers-store (atom []))

(em/defsnippet tracker-list-item "templates/trackers-page.html"
  ["#tracker-list > tbody > *:first-child"]  [tracker]
  [".tracker-name"] (em/content (:name tracker))
  [".tracker-code"] (em/content (:tracker_code tracker))
  )

(defn- tracker-list [trackers]
  (map #(tracker-list-item %) trackers))

(em/defsnippet trackers-template "templates/trackers-page.html"
  ["#content"] [trackers]
  ["#tracker-list > tbody"] (em/content (tracker-list trackers)))

(em/defaction update-trackers-list [trackers]
  ["#tracker-list > tbody"] (em/content (tracker-list trackers)))
  
(defn- update-trackers [trackers-data]
  (let [trackers (:trackers trackers-data)]
    (debug "Got" (count trackers) "trackers")
    (reset! trackers-store trackers)
    (update-trackers-list @trackers-store)))

(defn- refresh-trackers []
  (info "refreshing trackers list")
  (api/get-trackers update-trackers nil))

(defmethod view/init-content :trackers []
  (refresh-trackers))

(defmethod view/content-template :trackers []
  (let [trackers @trackers-store]
    (debug "trackers-store contains" (count trackers) "trackers")
    (trackers-template trackers)
  ))

