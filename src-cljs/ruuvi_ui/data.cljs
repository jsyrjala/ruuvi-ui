(ns ruuvi-ui.data 
  (:require [ruuvi-ui.util :as util])
  (:use [jayq.util :only [clj->js]]
        [clojure.string :only [split]])
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

;; TODO this is slow, avoid!
;; use priority-map? mutable sorting in javascript?
(defn- sort-events [events]
  (sort-by :event_time events))

(defn- merge-events [old-events new-events]
  ;; OPTIMIZE assume that both event list are in order
  ;; concat if new-events is later than old events
  ;; (handle equal events on overlap)
  ;; otherwise do below merge/dedupe/sort
  (let [events (concat old-events new-events)
        sorted-events (sort-by :id events)
        grouped-events (partition-by :id sorted-events)
        deduped-events (map first grouped-events)]
    deduped-events))

(defn- update-path [existing-path events]
  (let [coordinates (map util/get-event-lat-lng events)
        coordinates (filter identity coordinates)]
    (if existing-path
      (let []
        ;; TODO optimize (recreating whole path is slow?)
        ;; if event_time for earliest new event is later or equal that
        ;; latest. 
        ;; old event => add to path, otherwise recreate whole path
        ;; Note that those events may also be identicals ->
        ;; must compare non identical events here.
        (.setLatLngs existing-path (clj->js coordinates)))
      (let [path (new js/L.Polyline (clj->js coordinates))]
        (.addTo path (@state :map-view)))) ))


(defn add-tracker-data [trackers-data]
  ;; Update tracker data to trackers-store. Overwrite existing
  ;; trackers.
  (let [trackers (:trackers trackers-data)]
    (swap! state #(update-in % [:trackers]
                             (fn [old-list]
                               (reduce (fn [sum tracker]
                                         (update-in sum [(:id tracker) :tracker]
                                                    (fn [_] tracker)))
                                       old-list
                                       trackers))))))

(defn- update-events-to-trackers [trackers tracker-id session-id new-events]
  (let [;; update events per tracker and session basis
        trackers (update-in trackers [tracker-id session-id :events]
                            #(sort-events (merge-events % new-events)))
        
        ;; update paths per tracker and session basis
        events (get-in trackers [tracker-id session-id :events])
        trackers (update-in trackers [tracker-id session-id :path]
                            (fn [existing-path]
                              (update-path existing-path events)) )
        
        ;; update current location marker per tracker
        trackers (update-in trackers [tracker-id :marker]
                            (fn [existing-marker]
                              (util/update-marker existing-marker
                                             (util/get-first-location events)
                                             (@state :map-view) {})))
        
        ;; update latest event timestamps per tracker basic
        trackers (update-in trackers [tracker-id :latest-event-time]
                            (fn [time]
                              (apply max (conj (map :event_time new-events) time))) )
        trackers (update-in trackers [tracker-id :latest-store-time]
                            (fn [time]
                              (apply max (conj (map :store_time new-events) time))) )
        ]
    trackers
    ))

(defn- add-tracker-event-data [tracker-id session-id new-events]
  ;; merge new-events with old events and remove dupes
  (swap! state (fn [old-state]
                 (update-in old-state [:trackers]
                            #(update-events-to-trackers % tracker-id session-id new-events)
                            ))))

(defn- tracker-session-grouping [event]
  (let [tracker-id (:tracker_id event)
        session-id (:event_session_id event)]
    ;; TODO group-by doesn't work if [tracker-id event-id] is returned. it
    ;; returns undefined
    (str tracker-id "/" session-id)))

(defn- tracker-session-degrouping [grouping]
  (let [parts (split grouping "/")
        tracker-id (parts 0)
        session-id (parts 1)]
    [(or tracker-id nil) (or session-id nil)] ))

(defn add-event-data [events-data]
  ;; group events by tracker_id and event_session_id
  (let [events (:events events-data)
        grouped (group-by tracker-session-grouping events)]
    (doall
     (map (fn [g]
            (let [[tracker-id session-id] (tracker-session-degrouping (key g))]
              (add-tracker-event-data tracker-id session-id (val g)))) grouped)) ))

