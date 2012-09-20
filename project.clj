(defproject ruuvi-ui "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :plugins [[lein-cljsbuild "0.2.7"]
            [lein-midje "2.0.0-SNAPSHOT"]
            ]
  :dependencies [[org.clojure/clojure "1.4.0"]
                 ;; ClojureScript
                 [enfocus "1.0.0-alpha3"]

                 ;; JavaScript
                 [jayq "0.1.0-alpha4"]

                 ;; web server
                 [compojure "1.1.1"]
                 [ring/ring-core "1.1.1"]
                 [ring/ring-servlet "1.1.1"]
                 [ring/ring-jetty-adapter "1.1.1"]
                 [ring/ring-devel "1.1.1"]
                 [amalloy/ring-gzip-middleware "0.1.2"]
                 
                 ;; logging
                 [org.clojure/tools.logging "0.2.4"
                  :exclusions
                  [log4j/log4j
                   commons-logging/commons-logging
                   org.slf4j/slf4j-api
                   org.slf4j/slf4j-log4j12]]
                 [ch.qos.logback/logback-classic "1.0.6"]
                 [org.slf4j/log4j-over-slf4j "1.6.6"]
                 ]
  :main ruuvi-ui-server.server
  :cljsbuild {
              ;;:crossovers [depr.crossovers]
              ;;:crossover-jar false
              :builds {
                       
                       :dev
                       {:source-path "src-cljs"
                        ;;:jar true
                        :compiler {:output-to "resources/public/js/ruuvi-tracker-ui-debug.js"
                                   :pretty-print true
                                   :externs ["externs/jquery.js"]
                                   }}

                       :prod
                       {:source-path "src-cljs"                        
                        :compiler {:output-to "resources/public/js/ruuvi-tracker-ui.js"
                                   :optimizations :advanced
                                   :pretty-print true
                                   :externs ["externs/jquery.js"]
                                   :sourcemap true
                                   }}
                       
                       
              }}
              

  )
