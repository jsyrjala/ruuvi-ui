(ns ruuvi-ui.views.debug
  (:require [enfocus.core :as ef]
            [ruuvi-ui.util :as util]
            [ruuvi-ui.api :as api]
            [ruuvi-ui.view :as view]
            [ruuvi-ui.views.navigation :as navi]
            )
  (:use [ruuvi-ui.log :only [debug info warn error]])
  (:require-macros [enfocus.macros :as em])
  )

(em/defsnippet debug-list-item "templates/debug-page.html" ["#container > tbody > *:first-child"]
  [item]
  [".key"] (em/content (:key item))
  [".value"] (em/content (:value item))
  )

(em/deftemplate debug-template "templates/debug-page.html" [items]
  ["#container > tbody"] (em/content (map #(debug-list-item %) items))
  )

(em/defaction update-debug-list [items]
  ["#container > tbody"] (em/content (map #(debug-list-item %) items))
  )

(defmethod view/init-content :debug []
  (let [items [{:key "k1" :value "v1"}
               {:key "k2" :value "v2"}
               {:key "k3-" :value "v3"}
               {:key "k3-" :value "v3"}]]
    (info "init-content" items) 
    (update-debug-list items)
    ))

(defmethod view/content-template :debug []
  (let [items [{:key "k1" :value "v1"}
               {:key "k2" :value "v2"}
               {:key "k3-" :value "v3"}]]
    (debug-template items)))

