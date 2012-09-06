(ns ruuvi-ui.app
  (:require [enfocus.core :as ef]
            [ruuvi-ui.map :as map]
            [ruuvi-ui.api :as api]
            [ruuvi-ui.util :as util]
            [ruuvi-ui.view :as view]
            )
  (:require-macros [enfocus.macros :as em])
  )

(defn setup [canvas-id]
  (start-map-view canvas-id)
  (start-buttons)
  (map/locate)
  )

(defn- load-internal [active-page]
  (view/load-navigation active-page)
  (view/load-content active-page)
  )

(defn- get-current-page
  [hash]
  (if (or (empty? hash) (= "#" hash))
    :index
    (let [parts (re-seq #"#([a-z]+).*" hash)]
      (if parts
        (last (first parts))
        :error))))

(defn ^:extern load-page
  ([] (load-page (let [page (get-current-page js/window.location.hash)]
                   (util/log "page" page)
                   page)))
  ([active-page]
     (let [active-page (if active-page (keyword active-page) :map)]
       (em/wait-for-load (load-internal active-page)))))
