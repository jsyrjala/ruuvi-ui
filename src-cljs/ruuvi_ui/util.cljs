(ns ruuvi-ui.util
  (:require [jayq.core :as jquery])
  (:use [jayq.util :only [clj->js]])
  )

(defn log
  ([msg] (js/console.log (clj->js msg)))
  ([msg1 msg2] (js/console.log (clj->js msg1) (clj->js msg2)))
  ([msg1 msg2 msg3] (js/console.log (clj->js msg1) (clj->js msg2) (clj->js msg3)))
  ([msg1 msg2 msg3 msg4] (js/console.log (map clj->js [msg1 msg2 msg3 msg4])))
  
)

  
(defn log-error [operation & errors]
  (log (str operation " failed") errors))
