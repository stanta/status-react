(ns status-im.acquisition.persistance
  (:require [re-frame.core :as re-frame]
            [taoensso.timbre :as log]
            ["@react-native-community/async-storage" :default async-storage]))

(def referrer-decision-key "acquisition-referrer-decision")
(def tx-store-key "acquisition-watch-tx")

(re-frame/reg-fx
 ::set-referrer-decision
 (fn [decision]
   (-> ^js async-storage
       (.setItem referrer-decision-key decision)
       (.catch (fn [error]
                 (log/error "[async-storage]" error))))))

(defn get-referrer-decision [on-success]
  (-> ^js async-storage
      (.getItem referrer-decision-key)
      (.then (fn [^js data]
               (on-success data)))
      (.catch (fn [error]
                (log/error "[async-storage]" error)))))

(re-frame/reg-fx
 ::check-tx-state
 (fn [on-success]
   (-> ^js async-storage
       (.getItem tx-store-key)
       (.then (fn [^js tx]
                (on-success tx)))
       (.catch (fn [error]
                 (log/error "[async-storage]" error))))))

(re-frame/reg-fx
 ::set-wtach-tx
 (fn [tx]
   (-> ^js async-storage
       (.setItem tx-store-key tx)
       (.catch (fn [error]
                 (log/error "[async-storage]" error))))))
