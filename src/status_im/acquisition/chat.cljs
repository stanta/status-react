(ns status-im.acquisition.chat
  (:require [status-im.utils.fx :as fx]
            [status-im.acquisition.claim :as claim]
            [status-im.ethereum.core :as ethereum]
            [status-im.acquisition.gateway :as gateway]
            [status-im.chat.models :as chat]))

(fx/defn start-acquisition
  [{:keys [db] :as cofx} {:keys [key] :as referrer}]
  (fx/merge cofx
            {:db (assoc-in db [:acquisition :chat-referrer key] referrer)}
            (chat/start-chat key)))

(fx/defn accept-pack
  {:events [::advertiser-decision]}
  [{:keys [db] :as cofx} decision]
  (let [referral (get-in db [:acquisition :referrer])
        payload  {:chat_key    (get-in db [:multiaccount :public-key])
                  :address     (ethereum/default-address db)
                  :invite_code referral}]
    (fx/merge cofx
              {:db (update db :acquisition dissoc :chat-referrer)}
              (gateway/handle-acquisition {:message    payload
                                           :method     "PATCH"
                                           :url        (gateway/get-url :clicks referral)
                                           :on-success ::claim/success-starter-pack-claim}))))
