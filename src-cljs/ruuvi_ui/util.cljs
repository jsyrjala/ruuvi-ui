(ns ruuvi-ui.util
  (:require [jayq.core :as jquery])
  )

(defn log
  ([msg] (js/console.log msg))
  ([msg1 msg2] (js/console.log msg1 msg2)))
  
(defn log-error [operation & errors]
  (log (str operation " failed") errors))
