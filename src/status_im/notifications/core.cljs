(ns status-im.notifications.core
  (:require [re-frame.core :as re-frame]
            [status-im.utils.fx :as fx]
            [status-im.native-module.core :as status]
            ["react-native-push-notification" :as rn-pn]))

(re-frame/reg-fx
 ::request-permission
 (fn []
   (.requestPermissions ^js rn-pn)))

(fx/defn request-permission
  {:events [::request-permission]}
  [_]
  {::request-permission true})

(re-frame/reg-fx
 ::local-notification
 (fn [{:keys [title message]}]
   (.localNotification ^js rn-pn
                       #js {:title   title
                            :message message})))

(re-frame/reg-fx
 ::enable
 (fn [_]
   (status/enable-notifications)))

(re-frame/reg-fx
 ::disable
 (fn [_]
   (status/disable-notifications)))
