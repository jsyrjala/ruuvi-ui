(ns ruuvi-ui.view
  (:require [enfocus.core :as ef]
            )
  (:use [ruuvi-ui.log :only [debug info warn error]])
  (:require-macros [enfocus.macros :as em]))

(em/deftemplate error-template "templates/error-page.html" [])

(defmulti content-template identity)

(defmethod content-template :default []
  ;; TODO warn should have page name
  (warn (str "Page was not found"))
  (error-template))

(defmulti init-content identity)

(defmethod init-content :default []
  ;; default is to do nothing
  )

(em/defaction display-content-template [page]
  ["#content"] (em/content (content-template page)))

(defn display-content [page]
  (display-content-template page)
  (init-content page))
