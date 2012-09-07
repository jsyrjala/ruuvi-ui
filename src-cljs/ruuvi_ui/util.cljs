(ns ruuvi-ui.util
  (:require [jayq.core :as jquery])
  (:use [jayq.util :only [clj->js]])
  )

(defn log
  ([msg] (js/console.log (clj->js msg)))
  ([msg1 msg2] (js/console.log (clj->js msg1) (clj->js msg2))))
  
(defn log-error [operation & errors]
  (log (str operation " failed") errors))
