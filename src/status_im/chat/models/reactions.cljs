(ns status-im.chat.models.reactions
  (:require [status-im.constants :as constants]
            [re-frame.core :as re-frame]
            [status-im.utils.fx :as fx]
            [status-im.waku.core :as waku]
            [taoensso.timbre :as log]
            [status-im.multiaccounts.model :as multiaccounts.model]
            [status-im.transport.message.protocol :as message.protocol]
            [status-im.data-store.reactions :as data-store.reactions]))

(defn process-reactions
  [reactions new-reactions]
  ;; TODO(Ferossgp): handling own reaction in subscription could be expensive,
  ;; for better performance we can here separate own reaction into 2 maps
  (reduce
   (fn [acc {:keys [chat-id message-id emoji-id emoji-reaction-id retracted]
             :as   reaction}]
     ;; NOTE(Ferossgp): For a better performance, better to not keep in db all retracted reactions
     ;; retraction will always come after the reaction so there shouldn't be a conflict
     (if retracted
       (update-in acc [chat-id message-id emoji-id] dissoc emoji-reaction-id)
       (assoc-in acc [chat-id message-id emoji-id emoji-reaction-id] reaction)))
   reactions
   new-reactions))

(defn- earlier-than-deleted-at?
  [{:keys [db]} {:keys [chat-id clock-value]}]
  (let [{:keys [deleted-at-clock-value]}
        (get-in db [:chats chat-id])]
    (>= deleted-at-clock-value clock-value)))

(defn extract-chat-id
  [_ reaction]
  ;; TODO: Implement when go side is ready
  (get reaction :chat-id))

(fx/defn receive-signal
  [{:keys [db] :as cofx} reactions]
  (when-let [chat-id (extract-chat-id cofx reactions)]
    (let [reactions (->> reactions
                         (map #(assoc % :chat-id chat-id))
                         (filter (partial earlier-than-deleted-at? cofx)))]
      {:db (update :reactions process-reactions reactions chat-id)})))

(fx/defn load-more-reactions
  [{:keys [db] :as cofx} cursor]
  (when-let [current-chat-id (:current-chat-id db)]
    (when-let [session-id (get-in db [:pagination-info current-chat-id :messages-initialized?])]
      (data-store.reactions/reactions-by-chat-id-rpc
       (waku/enabled? cofx)
       current-chat-id
       cursor
       constants/default-number-of-messages
       #(re-frame/dispatch [::reactions-loaded current-chat-id session-id %])
       #(log/error "failed loading reactions" current-chat-id %)))))

(fx/defn reactions-loaded
  {:events [::reactions-loaded]}
  [{{:keys [current-chat-id] :as db} :db :as cofx}
   chat-id
   session-id
   {:keys [cursor reactions]}]
  (when-not (or (nil? current-chat-id)
                (not= chat-id current-chat-id)
                (and (get-in db [:pagination-info current-chat-id :messages-initialized?])
                     (not= session-id
                           (get-in db [:pagination-info current-chat-id :messages-initialized?]))))
    (let [reactions-w-chat-id (map #(assoc % :chat-id chat-id) reactions)]
      {:db (update db :reactions process-reactions reactions-w-chat-id)})))


;; Send reactions


(fx/defn send-emoji-reaction
  {:events [::send-emoji-reaction]}
  [{{:keys [current-chat-id] :as db} :db :as cofx} reaction]
  (message.protocol/send-reaction cofx
                                  (assoc reaction :chat-id current-chat-id)))

(fx/defn send-retract-emoji-reaction
  {:events [::send-emoji-reaction-retraction]}
  [{{:keys [current-chat-id reactions] :as db} :db :as cofx} reaction]
  (message.protocol/send-retract-reaction cofx
                                          (assoc reaction :chat-id current-chat-id)))

(fx/defn reaction-sent
  {:events [:transport/reaction-sent]}
  [{:keys [db] :as cofx} reaction id]
  (let [current-public-key  (multiaccounts.model/current-public-key cofx)
        optimistic-reaction (assoc reaction
                                   :emoji-reaction-id id
                                   :from  current-public-key
                                   :retracted false)]
    {:db (update db :reactions process-reactions [optimistic-reaction])}))

(fx/defn retraction-sent
  {:events [:transport/retraction-sent]}
  [{:keys [db]} reaction]
  (let [optimistic (assoc reaction :retracted true)]
    {:db (update db :reactions process-reactions [optimistic])}))
