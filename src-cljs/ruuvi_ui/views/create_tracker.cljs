(ns ruuvi-ui.views.create_tracker
  (:require [enfocus.core :as ef]
            [ruuvi-ui.api :as api]
            [ruuvi-ui.view :as view]
            [ruuvi-ui.views.navigation :as navi]
            )
  (:require-macros [enfocus.macros :as em])
  (:use [ruuvi-ui.log :only [debug info warn error]
         jayq.core :only [$]])
  )

(em/deftemplate create-tracker-template "templates/create-tracker-page.html" [])

(defn- create-tracker [event]
  (let [name (.val (js/$ "#tracker-name"))
        code (.val (js/$ "#tracker-code"))
        shared-secret (.val (js/$ "#tracker-secret"))
        demo-password (.val (js/$ "#access-password"))]
    (.preventDefault event)
    (.stopPropagation event)
    (if (= demo-password (str "#" (str "ruuvi") "penkki"))
           (api/create-tracker {:name name
                                :code code
                                :shared-secret shared-secret
                                :success-fn #(navi/display-page :trackers)
                                })
           (info "Password is not correct.")
    )))

(em/defaction init-components []
  [:#create-tracker-form] (em/listen :submit create-tracker))

(defmethod view/init-content :create-tracker []
  (init-components))

(defmethod view/content-template :create-tracker []
  (create-tracker-template))

