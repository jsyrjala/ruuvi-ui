(ns ruuvi-ui.storage
  (:use [ruuvi-ui.log :only [debug info warn error]]
        [jayq.util :only [clj->js]]
        [clojure.walk :only [keywordize-keys]])
  )

;; 1. store to local storage if available
;; 2. store to cookie? how?
;; 3. store to closure

;; TODO locking

(defn- to-string [data]
  (js/JSON.stringify (clj->js data)))

(defn- storage-key [key]
  (to-string (str "ruuvitracker/" (name key))))

(defn- to-clj [data]
  (keywordize-keys (js->clj (js/JSON.parse data))))

(defn- store-localstorage [key value]
  (debug "store (localStorage)" key value)
  (.setItem js/localStorage (storage-key key) (to-string value)))

(defn- store-cookie [key value]
  (error "Cookie storage not implemented yet")
  )

(defn- fetch-localstorage [key]
  (let [value (to-clj (.getItem js/localStorage (storage-key key)))]
    (debug "fetch (localStorage)" key value)
    value
    ))

(defn- fetch-cookie [key]
  (error "Cookie storage not implemented yet")
  )

(defn- delete-localstorage [key]
  (to-clj (.removeItem js/localStorage (storage-key key))))

(defn- delete-cookie [key]
  (error "Cookie storage not implemented yet")
  )

(defn store [key value]
  (if js/localStorage
    (store-localstorage key value)
    (store-cookie key value)))

(defn fetch [key]
  (if js/localStorage
    (fetch-localstorage key)
    (fetch-cookie key)))

(defn delete [key]
  (if js/localStorage
    (delete-localstorage key)
    (delete-cookie key)))

;; TODO locking
(defn update [key update-fn & args]
  (let [old-value (fetch key)
        new-value (apply update-fn (concat [old-value] args))]
    (store key new-value)))
