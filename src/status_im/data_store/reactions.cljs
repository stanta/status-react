(ns status-im.data-store.reactions
  (:require [clojure.set :as clojure.set]
            [status-im.ethereum.json-rpc :as json-rpc]))

(defn ->rpc [message]
  (-> message
      (clojure.set/rename-keys {:chat-id     :chatId
                                :clock-value :clock})))

(defn <-rpc [message]
  (-> message
      (clojure.set/rename-keys {:messageId :message-id
                                :emojiId   :emojiId})))

(defn reactions-by-chat-id-rpc [waku-enabled?
                                chat-id
                                cursor
                                limit
                                on-success
                                on-failure]
  {::json-rpc/call [{:method     (json-rpc/call-ext-method waku-enabled? "chatReactions")
                     :params     [chat-id cursor limit]
                     :on-success (fn [result]
                                   (on-success (update result :reactions #(map <-rpc %))))
                     :on-failure on-failure}]})
