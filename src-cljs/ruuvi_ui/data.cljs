(ns ruuvi-ui.data
 
  )
;; TODO support several maps?


;; {:trackers {...}
;;  :map {...}
;;  :self-location {:location ...
;;             :marker ...
;;            }
;;  :selected-trackers {...} ?
;; }


;; {tracker-id1 {:tracker <tracker-object>
;;               :latest-event-time timestamp
;;               :latest-store-time timestamp
;;               :marker <marker-object>
;;               session-id1 {:events [event1 event2]
;;                            :path <path-object>}
;; }

(def state (atom {:map-view nil
                  :self-location {}
                  :trackers {}
                  }
                 ))

