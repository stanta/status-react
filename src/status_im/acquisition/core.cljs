(ns status-im.acquisition.core
  (:require [clojure.string :as cstr]
            [re-frame.core :as re-frame]
            [reagent.ratom :refer [make-reaction]]
            [status-im.utils.fx :as fx]
            [status-im.ethereum.json-rpc :as json-rpc]
            [status-im.ethereum.contracts :as contracts]
            [status-im.utils.money :as money]
            [status-im.acquisition.chat :as chat]
            [status-im.acquisition.advertiser :as advertiser]
            [status-im.acquisition.persistance :as persistence]
            [status-im.acquisition.gateway :as gateway]
            ["react-native-device-info" :refer [getInstallReferrer]]))

(def advertiser-type "advertiser")
(def chat-type "chat")

(defn- split-param [param]
  (->
   (cstr/split param #"=")
   (concat (repeat ""))
   (->>
    (take 2))))

(defn- url-decode
  [string]
  (some-> string str (cstr/replace #"\+" "%20") (js/decodeURIComponent)))

(defn- query->map
  [qstr]
  (when-not (cstr/blank? qstr)
    (some->> (cstr/split qstr #"&")
             seq
             (mapcat split-param)
             (map url-decode)
             (apply hash-map))))

(defn parse-referrer
  "Google return query params for referral with all utm tags"
  [referrer]
  (-> referrer query->map (get "referrer")))

(fx/defn handle-registration
  [cofx {:keys [message on-success]}]
  (gateway/handle-acquisition cofx
                              {:message    message
                               :on-success on-success
                               :method     "POST"
                               :url        (gateway/get-url :registrations nil)}))

(re-frame/reg-fx
 ::get-referrer
 (fn [external-referrer]
   (persistence/get-referrer-decision
    (fn [^js data]
      (if external-referrer
        (re-frame/dispatch [::has-referrer data external-referrer])
        (-> (getInstallReferrer)
            (.then (fn [install-referrer]
                     (when (and (seq (parse-referrer install-referrer))
                                (not= install-referrer "unknown"))
                       (re-frame/dispatch [::has-referrer data  (parse-referrer install-referrer)]))))))))))

(fx/defn referrer-registered
  {:events [::referrer-registered]}
  [{:keys [db] :as cofx} referrer {:keys [type attributed] :as referrer-meta}]
  (when-not attributed
    (fx/merge cofx
              {:db (assoc-in db [:acquisition :metadata] referrer-meta)}
              (cond
                (= type advertiser-type)
                (advertiser/start-acquisition referrer-meta)

                (= type chat-type)
                (chat/start-acquisition referrer-meta)))))

(fx/defn has-referrer
  {:events [::has-referrer]}
  [{:keys [db] :as cofx} decision referrer]
  (when referrer
    (cond
      (nil? decision)
      (fx/merge cofx
                {:db (assoc-in db [:acquisition :referrer] referrer)}
                (gateway/get-referrer
                 referrer
                 (fn [resp]
                   [::referrer-registered referrer resp])))

      (= "accept" decision)
      {::persistence/check-tx-state (fn [tx]
                                      (when-not (nil? tx)
                                        (re-frame/dispatch [::add-tx-watcher tx])))})))

(fx/defn app-setup
  {}
  [_]
  {::get-referrer nil})

;; Starter pack

(fx/defn get-starter-pack-amount
  {:events [::starter-pack-amount]}
  [{:keys [db]} [_ eth-amount tokens tokens-amount sticker-packs]]
  {:db (assoc-in db [:acquisition :starter-pack :pack]
                 {:eth-amount    (money/wei->ether eth-amount)
                  :tokens        tokens
                  :tokens-amount (mapv money/wei->ether tokens-amount)
                  :sticker-packs sticker-packs})})

(fx/defn starter-pack
  {:events [::starter-pack]}
  [{:keys [db]}]
  (let [contract (contracts/get-address db :status/acquisition)]
    {::json-rpc/eth-call [{:contract   contract
                           :method     "getPack()"
                           :outputs    ["address" "uint256" "address[]" "uint256[]" "uint256[]"]
                           :on-success #(re-frame/dispatch [::starter-pack-amount (vec %)])}]}))

(re-frame/reg-sub-raw
 ::starter-pack
 (fn [db]
   (re-frame/dispatch [::starter-pack])
   (make-reaction
    (fn []
      (get-in @db [:acquisition :starter-pack :pack])))))
