(ns status-im.ui.screens.add-new.new-chat.views
  (:require [re-frame.core :as re-frame]
            [status-im.i18n :as i18n]
            [status-im.multiaccounts.core :as multiaccounts]
            [status-im.ui.components.chat-icon.screen :as chat-icon]
            [status-im.ui.components.colors :as colors]
            [status-im.ui.components.icons.vector-icons :as vector-icons]
            [quo.core :as quo]
            [status-im.ui.components.list.views :as list]
            [status-im.ui.components.react :as react]
            [status-im.ui.components.topbar :as topbar]
            [status-im.ui.screens.add-new.new-chat.styles :as styles]
            [status-im.utils.debounce :as debounce]
            [status-im.utils.utils :as utils])
  (:require-macros [status-im.utils.views :as views]))

(defn- render-row [row _ _]
  [quo/list-item
   {:title    (multiaccounts/displayed-name row)
    :icon     [chat-icon/contact-icon-contacts-tab
               (multiaccounts/displayed-photo row)]
    :chevron  true
    :on-press #(re-frame/dispatch [:chat.ui/start-chat
                                   (:public-key row)])}])

(defn- icon-wrapper [color icon]
  [react/view
   {:style {:width            32
            :height           32
            :border-radius    25
            :align-items      :center
            :justify-content  :center
            :background-color color}}
   icon])

(defn- input-icon
  [state new-contact?]
  (let [icon (if new-contact? :main-icons/add :main-icons/arrow-right)]
    (case state
      :searching
      [icon-wrapper colors/gray
       [react/activity-indicator {:color colors/white-persist}]]

      :valid
      [react/touchable-highlight
       {:on-press #(debounce/dispatch-and-chill [:contact.ui/contact-code-submitted new-contact?] 3000)}
       [icon-wrapper colors/blue
        [vector-icons/icon icon {:color colors/white-persist}]]]

      [icon-wrapper colors/gray
       [vector-icons/icon icon {:color colors/white-persist}]])))

(defn get-validation-label [value]
  (case value
    :invalid
    (i18n/label :t/user-not-found)
    :yourself
    (i18n/label :t/can-not-add-yourself)))

(views/defview new-chat []
  (views/letsubs [contacts      [:contacts/active]
                  {:keys [state ens-name public-key error]} [:contacts/new-identity]]
    [react/view {:style {:flex 1}}
     [topbar/topbar
      {:title       :t/new-chat
       :modal?      true
       :accessories [{:icon                :qr
                      :accessibility-label :scan-contact-code-button
                      :handler             #(re-frame/dispatch [:qr-scanner.ui/scan-qr-code-pressed
                                                                {:title   (i18n/label :t/new-contact)
                                                                 :handler :contact/qr-code-scanned}])}]}]
     [react/view {:flex-direction :row
                  :padding        16}
      [react/view {:flex          1
                   :padding-right 16}
       [quo/text-input
        {:on-change-text
         #(do
            (re-frame/dispatch [:set-in [:contacts/new-identity :state] :searching])
            (debounce/debounce-and-dispatch [:new-chat/set-new-identity %] 600))
         :on-submit-editing
         #(when (= state :valid)
            (debounce/dispatch-and-chill [:contact.ui/contact-code-submitted false] 3000))
         :placeholder         (i18n/label :t/enter-contact-code)
         :show-cancel         false
         :accessibility-label :enter-contact-code-input
         :auto-capitalize     :none
         :return-key-type     :go}]]
      [react/view {:justify-content :center
                   :align-items     :center}
       [input-icon state false]]]
     [react/view {:min-height 30 :justify-content :flex-end}
      [react/text {:style styles/message}
       (cond (= state :error)
             (get-validation-label error)
             (= state :valid)
             (str (when ens-name (str ens-name " • "))
                  (utils/get-shortened-address public-key))
             :else "")]]
     [list/flat-list {:data                      contacts
                      :key-fn                    :address
                      :render-fn                 render-row
                      :enableEmptySections       true
                      :keyboardShouldPersistTaps :always}]]))

(views/defview new-contact []
  (views/letsubs [{:keys [state ens-name public-key error]} [:contacts/new-identity]]
    [react/view {:style {:flex 1}}
     [topbar/topbar
      {:title       :t/new-contact
       :modal?      true
       :accessories [{:icon                :qr
                      :accessibility-label :scan-contact-code-button
                      :handler             #(re-frame/dispatch [:qr-scanner.ui/scan-qr-code-pressed
                                                                {:title        (i18n/label :t/new-contact)
                                                                 :handler      :contact/qr-code-scanned
                                                                 :new-contact? true}])}]}]
     [react/view {:flex-direction :row
                  :padding        16}
      [react/view {:flex          1
                   :padding-right 16}
       [quo/text-input
        {:on-change-text
         #(do
            (re-frame/dispatch [:set-in [:contacts/new-identity :state] :searching])
            (debounce/debounce-and-dispatch [:new-chat/set-new-identity %] 600))
         :on-submit-editing
         #(when (= state :valid)
            (debounce/dispatch-and-chill [:contact.ui/contact-code-submitted true] 3000))
         :placeholder         (i18n/label :t/enter-contact-code)
         :show-cancel         false
         :accessibility-label :enter-contact-code-input
         :auto-capitalize     :none
         :return-key-type     :go}]]
      [react/view {:justify-content :center
                   :align-items     :center}
       [input-icon state true]]]
     [react/view {:min-height 30 :justify-content :flex-end}
      [react/text {:style styles/message}
       (cond (= state :error)
             (get-validation-label error)
             (= state :valid)
             (str (when ens-name (str ens-name " • "))
                  (utils/get-shortened-address public-key))
             :else "")]]]))
