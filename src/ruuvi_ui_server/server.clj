(ns ruuvi-ui-server.server
  (:require [compojure.core :as compojure]
            [ring.adapter.jetty :as jetty]
            [compojure.route :as route]
            [compojure.handler :as handler]
            [ring.middleware.reload :as reload]
            [ring.middleware.stacktrace :as stacktrace]
            [ring.middleware.gzip :as gzip]
            )
  (:use [clojure.tools.logging :only (debug info warn error)])
  )

(defn- wrap-add-html-suffix
  "Adds .html URI:s without dots and without / ending"
  [handler]
  (fn [req]
    (handler
     (update-in req [:uri]
                #(if (and (not (.endsWith % "/")) (< (.indexOf % ".") 0))
                   (str % ".html")
                   %)))))

(defn- wrap-dir-index
  "Convert paths ending in / to /index.html"
  [handler]
  (fn [req]
    (handler
     (update-in req [:uri]
                #(if (.endsWith % "/" )
                   (str % "index.html")
                   %)))))

(compojure/defroutes main-routes
  (route/resources "/")
  (route/not-found "Resource not found."))

(defn wrap-request-logging [handler]
  (fn [{:keys [request-method uri] :as req}]
    (let [resp (handler req)]
      (info "Processing" request-method uri)
      resp)))

(def app
  (handler/site (-> main-routes
                    wrap-add-html-suffix
                    wrap-dir-index
                    wrap-request-logging
                    (reload/wrap-reload '(ruuvi-ui.server))
                    (stacktrace/wrap-stacktrace)
                    (gzip/wrap-gzip)
                    )))

(defn -main [& args]
  (jetty/run-jetty app {:port 8081}))

