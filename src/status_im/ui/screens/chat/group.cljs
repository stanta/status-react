(ns status-im.ui.screens.chat.group
  (:require [re-frame.core :as re-frame]
            [quo.core :as quo]
            [status-im.ui.components.react :as react]
            [status-im.utils.universal-links.core :as links]
            [status-im.ui.screens.chat.styles.main :as style]
            [status-im.i18n :as i18n]
            [status-im.ui.components.list-selection :as list-selection]
            [status-im.ui.components.colors :as colors]
            [reagent.core :as reagent]
            [clojure.string :as string])
  (:require-macros [status-im.utils.views :refer [defview letsubs]]))

(defn join-chat-button [chat-id]
  [quo/button
   {:type     :secondary
    :on-press #(re-frame/dispatch [:group-chats.ui/join-pressed chat-id])}
   (i18n/label :t/join-group-chat)])

(defn decline-chat [chat-id]
  [react/touchable-highlight
   {:on-press
    #(re-frame/dispatch [:group-chats.ui/leave-chat-confirmed chat-id])}
   [react/text {:style style/decline-chat}
    (i18n/label :t/group-chat-decline-invitation)]])

;;TODO event handlers
(defn request-membership [group-chat-status]
  (let [message (reagent/atom "")]
    (fn []
      [react/view {:margin-horizontal 16 :margin-top 10 :margin-bottom 40}
       (cond
         (= group-chat-status :request-pending)
         [quo/button
          {:type     :secondary
           :disabled true}
          "Request pending…"]

         (= group-chat-status :request-declined)
         [react/view
          [react/text {:style {:align-self :center :margin-bottom 30}}
           "Membership request was declined"]
          [quo/button
           {:type     :secondary
            :on-press #(re-frame/dispatch [:todo])}
           "Retry"]
          [quo/button
           {:type     :secondary
            :on-press #(re-frame/dispatch [:todo])}
           "Remove group"]]
         :default
         [react/view
          [react/text "Introduce yourself with a brief message"]
          [quo/text-input {:placeholder "Message"
                           :on-change-text #(reset! message %)
                           :multiline true
                           :container-style {:margin-top 10 :margin-bottom 16}}]
          [react/text {:style {:align-self :flex-end :margin-bottom 30}}
           (str (count @message) "/100")]
          [quo/button
           {:type     :secondary
            :disabled (string/blank? @message)
            :on-press #(re-frame/dispatch [:todo])}
           "Request membership"]])])))

(defview group-chat-footer
  [chat-id group-chat-status]
  (letsubs [{:keys [joined?]} [:group-chat/inviter-info chat-id]]
    (if group-chat-status
      [request-membership group-chat-status]
      (when-not joined?
        [react/view {:style style/group-chat-join-footer}
         [react/view {:style style/group-chat-join-container}
          [join-chat-button chat-id]
          [decline-chat chat-id]]]))))

(def group-chat-description-loading
  [react/view {:style (merge style/intro-header-description-container
                             {:margin-bottom 36
                              :height        44})}
   [react/text {:style style/intro-header-description}
    (i18n/label :t/loading)]
   [react/activity-indicator {:animating true
                              :size      :small
                              :color     colors/gray}]])

(defview no-messages-group-chat-description-container [chat-id]
  (letsubs [{:keys [highest-request-to lowest-request-from]}
            [:mailserver/ranges-by-chat-id chat-id]]
    [react/nested-text {:style (merge style/intro-header-description
                                      {:margin-bottom 36})}
     (let [quiet-hours (quot (- highest-request-to lowest-request-from)
                             (* 60 60))
           quiet-time  (if (<= quiet-hours 24)
                         (i18n/label :t/quiet-hours
                                     {:quiet-hours quiet-hours})
                         (i18n/label :t/quiet-days
                                     {:quiet-days (quot quiet-hours 24)}))]
       (i18n/label :t/empty-chat-description-public
                   {:quiet-hours quiet-time}))
     [{:style    {:color colors/blue}
       :on-press #(list-selection/open-share
                   {:message
                    (i18n/label
                     :t/share-public-chat-text {:link (links/generate-link :public-chat :external chat-id)})})}
      (i18n/label :t/empty-chat-description-public-share-this)]]))

(defview pending-invitation-description
  [inviter-pk chat-name]
  (letsubs [inviter-name [:contacts/contact-name-by-identity inviter-pk]]
    [react/nested-text {:style style/intro-header-description}
     [{:style {:color colors/black}} inviter-name]
     (i18n/label :t/join-group-chat-description
                 {:username   ""
                  :group-name chat-name})]))

(defview joined-group-chat-description
  [inviter-pk chat-name]
  (letsubs [inviter-name [:contacts/contact-name-by-identity inviter-pk]]
    [react/nested-text {:style style/intro-header-description}
     (i18n/label :t/joined-group-chat-description
                 {:username   ""
                  :group-name chat-name})
     [{:style {:color colors/black}} inviter-name]]))

(defn created-group-chat-description [chat-name]
  [react/text {:style style/intro-header-description}
   (i18n/label :t/created-group-chat-description
               {:group-name chat-name})])

(defview group-chat-inviter-description-container [chat-id chat-name]
  (letsubs [{:keys [joined? inviter-pk]}
            [:group-chat/inviter-info chat-id]]
    (cond
      (not joined?)
      [pending-invitation-description inviter-pk chat-name]
      inviter-pk
      [joined-group-chat-description inviter-pk chat-name]
      :else
      [created-group-chat-description chat-name])))

(defn group-chat-membership-description []
  [react/text {:style {:text-align :center :margin-horizontal 30}}
   "Group membership requires you to be accepted by the group admin"])

(defn group-chat-description-container
  [{:keys [public?
           group-chat-status
           chat-id
           chat-name
           loading-messages?
           no-messages?]}]
  (cond loading-messages?
        group-chat-description-loading

        (and no-messages? public?)
        [no-messages-group-chat-description-container chat-id]

        group-chat-status
        [group-chat-membership-description]

        (not public?)
        [group-chat-inviter-description-container chat-id chat-name]))
