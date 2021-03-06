(ns ruuvi-ui.app
  (:require [enfocus.core :as ef]
            [ruuvi-ui.pages :as pages]
            )
  (:use [ruuvi-ui.log :only [debug info warn error]])
  (:require-macros [enfocus.macros :as em])
  )

(defn- load-internal [active-page]
  (pages/display-page active-page))

(defn- get-current-page
  [hash]
  (if (or (empty? hash) (= "#" hash))
    :index
    (let [parts (re-seq #"#([-_a-z]+).*" hash)]
      (if parts
        (last (first parts))
        :error))))

(defn ^:export load-app
  ([] (load-app (get-current-page js/window.location.hash)))
  ([active-page]
     (info "Starting application with page" active-page)
     (let [active-page (if active-page (keyword active-page) :map)]
       (em/wait-for-load (load-internal active-page)))))
