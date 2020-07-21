(ns status-im.ui.components.invite.views
  (:require [quo.core :as quo]
            [reagent.core :as reagent]
            [re-frame.core :as re-frame]
            [status-im.ui.components.chat-icon.screen :as chat-icon]
            [status-im.ui.components.toolbar :as toolbar]
            [status-im.utils.utils :as utils]
            [status-im.i18n :as i18n]
            [status-im.ethereum.tokens :as tokens]
            [status-im.utils.money :as money]
            [quo.design-system.spacing :as spacing]
            [quo.design-system.colors :as colors]
            [status-im.ui.components.topbar :as topbar]
            [status-im.ui.components.invite.events :as events]
            [status-im.acquisition.core :as acquisition]
            [quo.react-native :as rn]))

;; Select account sheet
(defn- render-account [current-account change-account]
  (fn [account]
    (let [{:keys [max-threshold attrib-count]}
          @(re-frame/subscribe [:invite/account-reward account])]
      [quo/list-item
       {:theme     :accent
        :active    (= (:address current-account) (:address account))
        :disabled  (and max-threshold attrib-count
                        (< max-threshold (inc attrib-count)))
        :accessory :radio
        :icon      [chat-icon/custom-icon-view-list (:name account) (:color account)]
        :title     (:name account)
        :subtitle  (utils/get-shortened-checksum-address (:address account))
        :on-press  #(change-account account)}])))

(defn- accounts-list [accounts current-account change-account]
  (fn []
    [rn/view {:flex 1}
     [rn/view {:style (merge (:base spacing/padding-horizontal)
                             (:tiny spacing/padding-vertical))}
      [quo/text {:align :center}
       (i18n/label :t/invite-select-account)]]
     [rn/flat-list {:data      accounts
                    :key-fn    :address
                    :render-fn (render-account current-account change-account)}]]))

;; Invite sheet

(defn- step [{:keys [number description]}]
  [rn/view {:style (merge
                    (:small spacing/padding-vertical)
                    {:flex-direction :row
                     :flex           1
                     :align-items    :center})}
   [rn/view {:style {:width           40
                     :height          40
                     :border-radius   20
                     :border-width    1
                     :justify-content :center
                     :align-items     :center
                     :border-color    (:ui-01 @colors/theme)}}
    [quo/text {:weight :bold
               :size   :large}
     number]]
   [rn/view {:padding-left (:base spacing/spacing)
             :flex         1}
    [quo/text (i18n/label description)]]])

(def steps-values [{:number      1
                    :description :t/invite-instruction-first}
                   {:number      2
                    :description :t/invite-instruction-second}
                   {:number      3
                    :description :t/invite-instruction-third}
                   {:number      4
                    :description :t/invite-instruction-fourth}])

(defn- referral-steps []
  [rn/view {:style (merge
                    (:tiny spacing/padding-vertical)
                    (:base spacing/padding-horizontal)
                    {:border-bottom-width 1
                     :border-bottom-color (:ui-01 @colors/theme)})}
   [rn/view {:style {:padding-top    (:small spacing/spacing)
                     :padding-bottom (:x-tiny spacing/spacing)}}
    [quo/text {:color :secondary}
     (i18n/label :t/invite-instruction)]]
   [rn/view {:flex 1}
    (for [s steps-values]
      ^{:key (str (:number s))}
      [step s])]])

(defn- referral-account []
  (let [visible (reagent/atom false)]
    (fn [{:keys [account accounts change-account]}]
      [rn/view {:style (:tiny spacing/padding-vertical)}
       [rn/view {:style (merge (:base spacing/padding-horizontal)
                               (:x-tiny spacing/padding-vertical))}
        [quo/text {:color :secondary}
         (i18n/label :t/invite-receive-account)]]
       [rn/modal {:visible     @visible
                  :transparent true}
        [quo/bottom-sheet {:visible?  true
                           :on-cancel #(reset! visible false)}
         [accounts-list accounts account (fn [a]
                                           (change-account a)
                                           (reset! visible false))]]]
       [quo/list-item
        {:icon     [chat-icon/custom-icon-view-list (:name account) (:color account)]
         :title    (:name account)
         :subtitle (utils/get-shortened-checksum-address (:address account))
         :on-press #(reset! visible true)}]])))

(defn referral-invite []
  (let [account* (reagent/atom nil)]
    (fn []
      (let [accounts        @(re-frame/subscribe [:accounts-without-watch-only])
            default-account @(re-frame/subscribe [:multiaccount/default-account])
            account         (or @account* default-account)
            reward          @(re-frame/subscribe [::events/default-reward])
            starter-pack    @(re-frame/subscribe [::acquisition/starter-pack])]
        [rn/view {:flex 1}
         [topbar/topbar {:modal?       true
                         :show-border? true
                         :title        (i18n/label :t/invite-friends)}]
         [rn/scroll-view {:flex 1}
          [quo/text {}
           (str "My reward: " reward)]
          [quo/text
           (str "User starte pack: " starter-pack)]
          [referral-steps]
          [referral-account {:account        account
                             :change-account #(reset! account* %)
                             :accounts       accounts}]]
         [toolbar/toolbar
          {:show-border? true
           :center
           [quo/button {:type     :secondary
                        :on-press #(re-frame/dispatch [::events/generate-invite
                                                       {:address (get account :address)}])}
            (i18n/label :t/invite-button)]}]]))))

(defn button []
  (let [reward @(re-frame/subscribe [::events/default-reward])]
    [rn/view {:style {:align-items :center}}
     [rn/view {:style (:tiny spacing/padding-vertical)}
      [quo/button {:on-press            #(re-frame/dispatch [::events/open-invite])
                   :accessibility-label :invite-friends-button}
       (i18n/label :t/invite-friends)]]
     [rn/view {:style (merge (:tiny spacing/padding-vertical)
                             (:base spacing/padding-horizontal))}
      (when reward
        [rn/view {:style {:flex-direction  :row
                          :align-items     :center
                          :justify-content :center}}
         [rn/view {:style (:tiny spacing/padding-horizontal)}
          (when-let [{:keys [source]} (tokens/symbol->icon :SNT)]
            [rn/image {:style  {:width  20
                                :height 20}
                       :source (source)}])]
         [quo/text {:align :center}
          (i18n/label :t/invite-reward {:value (money/wei->str :eth (get reward :eth-amount) "ETH")})]])]]))

(defn list-item [{:keys [accessibility-label]}]
  (let [amount @(re-frame/subscribe [::events/default-reward])]
    [quo/list-item
     {:theme               :accent
      :title               (i18n/label :t/invite-friends)
      :subtitle            (i18n/label :t/invite-reward {:value (money/wei->str :eth amount "SNT")})
      :icon                :main-icons/share
      :accessibility-label accessibility-label
      :on-press            #(do
                              (re-frame/dispatch [:bottom-sheet/hide])
                              (re-frame/dispatch [::events/open-invite]))}]))




