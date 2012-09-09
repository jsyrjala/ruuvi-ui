(ns ruuvi-ui.log
  (:use [jayq.util :only [clj->js]])
  )

(defn log
  ([msg] (js/console.log (clj->js msg)))
  ([msg1 msg2] (js/console.log (clj->js msg1) (clj->js msg2)))
  ([msg1 msg2 msg3] (js/console.log (clj->js msg1) (clj->js msg2) (clj->js msg3)))
  ([msg1 msg2 msg3 msg4] (js/console.log (clj->js msg1) (clj->js msg2) (clj->js msg3) (clj->js msg4)))
  ([msg1 msg2 msg3 msg4 msg5] (js/console.log (clj->js msg1) (clj->js msg2) (clj->js msg3) (clj->js msg4)
                                              (clj->js msg5)))
  ([msg1 msg2 msg3 msg4 msg5 msg6] (js/console.log (clj->js msg1) (clj->js msg2) (clj->js msg3) (clj->js msg4)
                                                   (clj->js msg5) (clj->js msg6)))
  ([msg1 msg2 msg3 msg4 msg5 msg6 msg7] (js/console.log (clj->js msg1) (clj->js msg2) (clj->js msg3) (clj->js msg4)
                                                   (clj->js msg5) (clj->js msg6) (clj->js msg7)))
  )

(defn- log-level [level msgs]
  (apply log (concat [level] msgs)))

(defn debug [& msgs]
  (log-level "DEBUG:" msgs))

(defn info [& msgs]
  (log-level "INFO: " msgs))
 
(defn warn [& msgs]
  (log-level "WARN: " msgs))

(defn error [& msgs]
  (log-level "ERROR:" msgs))
