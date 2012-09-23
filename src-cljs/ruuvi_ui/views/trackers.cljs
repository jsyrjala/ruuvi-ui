(ns ruuvi-ui.views.trackers
  (:require [enfocus.core :as ef]
            [ruuvi-ui.util :as util]
            [ruuvi-ui.storage :as storage]
            [ruuvi-ui.api :as api]
            [ruuvi-ui.view :as view]
            [ruuvi-ui.views.navigation :as navi]
            [ruuvi-ui.map-api :as map-api]
            )
  (:require-macros [enfocus.macros :as em])
  (:use [ruuvi-ui.log :only [debug info warn error]]
        [jayq.core :only [$ attr]]
        )
  )
;; - list of trackers is hold in an atom
;; - trackers from atom is shown when page is displayed
;; - after page display tracker refresh is restarted (AJAX)

;; TODO
;; - use singleton atom to hold the trackers
;; - store tracker list to html5 local storage (if available, storage constraints)
;; - refresh trackers periodically
;; - display latest activity for each tracker (2 min ago etc)
;; - localization for different languages
(def trackers-store (atom []))

;; TODO should return boolean
(defn selected-tracker? [tracker-id]
  (let [tracker-ids (storage/fetch :selected-trackers)]
    (some #{tracker-id} tracker-ids)))

(defn- toggle-tracker-selection [event tracker-id]
  (let [target ($ (.-currentTarget event))
        checked (attr target :checked)]
    (storage/update :selected-trackers
                    (fn [tracker-ids]
                      (let [tracker-ids (into #{} tracker-ids)]
                        (if checked
                          (conj tracker-ids tracker-id)
                          (disj tracker-ids tracker-id) ))))))

(em/defsnippet tracker-list-item "templates/trackers-page.html"
  ["#tracker-list > tbody > *:first-child"] [tracker]
  
  [".tracker-selected"] (if (selected-tracker? (:id tracker))
                          (em/set-attr :checked "checked")
                          (em/remove-attr :checked))
  [".tracker-selected"] (let [tracker-id (:id tracker)]
                          (em/listen :change #(toggle-tracker-selection % tracker-id)))
  [".tracker-name"] (em/content (:name tracker))
  [".tracker-code"] (em/content (:tracker_code tracker)))

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
    (update-trackers-list @trackers-store)
    ))

(defn- refresh-trackers []
  (info "Refreshing trackers list")
  (api/get-trackers update-trackers nil))

(defmethod view/init-content :trackers []
  (update-trackers-list @trackers-store)
  (refresh-trackers))

(defmethod view/content-template :trackers []
  (let [trackers @trackers-store]
    (trackers-template trackers) ))

