(ns ruuvi-ui.views.create_tracker
  (:require [enfocus.core :as ef]
            [ruuvi-ui.api :as api]
            [ruuvi-ui.view :as view]
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
    (api/create-tracker {:name name
                         :code code
                         :shared-secret shared-secret
                         })
  (.preventDefault event)
  (.stopPropagation event)
  ))

(em/defaction init-components []
  [:#create-tracker-button] (em/listen :click create-tracker))

(defmethod view/init-content :create-tracker []
  (init-components))

(defmethod view/content-template :create-tracker []
  (create-tracker-template))

