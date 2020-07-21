(ns status-im.ui.screens.home.views
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [status-im.i18n :as i18n]
            [status-im.react-native.resources :as resources]
            [status-im.ui.components.connectivity.view :as connectivity]
            [status-im.ui.components.icons.vector-icons :as icons]
            [status-im.ui.components.list.views :as list]
            [status-im.ui.components.react :as react]
            [status-im.ui.screens.home.styles :as styles]
            [status-im.ui.screens.home.views.inner-item :as inner-item]
            [status-im.ui.components.list-selection :as list-selection]
            [status-im.ui.components.colors :as colors]
            [status-im.ui.screens.add-new.new-public-chat.view :as new-public-chat]
            [quo.core :as quo]
            [status-im.ui.components.search-input.view :as search-input]
            [cljs-bean.core :as bean]
            [status-im.ui.components.invite.views :as invite]
            [status-im.ui.components.topbar :as topbar])
  (:require-macros [status-im.utils.views :as views]))

(defn welcome-image-wrapper []
  (let [dimensions (reagent/atom {})]
    (fn []
      [react/view {:on-layout (fn [^js e]
                                (reset! dimensions (bean/->clj (-> e .-nativeEvent .-layout))))
                   :style     {:align-items     :center
                               :justify-content :center
                               :flex            1}}
       (let [padding    0
             image-size (- (min (:width @dimensions) (:height @dimensions)) padding)]
         [react/image {:source      (resources/get-theme-image :welcome)
                       :resize-mode :contain
                       :style       {:width image-size :height image-size}}])])))

(defn welcome []
  [react/view {:style styles/welcome-view}
   [welcome-image-wrapper]
   [react/i18n-text {:style styles/welcome-text :key :welcome-to-status}]
   [react/view
    [react/i18n-text {:style styles/welcome-text-description
                      :key   :welcome-to-status-description}]]
   [react/view {:align-items :center :margin-bottom 50}
    [quo/button {:on-press #(re-frame/dispatch [:navigate-reset {:index               0
                                                                 :routes [{:name :tabs}]}])
                 :accessibility-label :lets-go-button}
     (i18n/label :t/lets-go)]]])

(defn home-tooltip-view []
  [react/view (styles/chat-tooltip)
   [react/view {:style {:flex-direction :row}}
    [react/view {:flex 1}
     [react/view {:style styles/empty-chats-header-container}
      [react/view {:style {:width       66 :position :absolute :top -6 :background-color colors/white
                           :align-items :center}}
       [react/image {:source (resources/get-image :empty-chats-header)
                     :style  {:width 50 :height 50}}]]]
     [react/touchable-highlight
      {:style               {:position :absolute :right  0  :top         0
                             :width    44        :height 44 :align-items :center :justify-content :center}
       :on-press            #(re-frame/dispatch [:multiaccounts.ui/hide-home-tooltip])
       :accessibility-label :hide-home-button}
      [icons/icon :main-icons/close-circle {:color colors/gray}]]]]
   [react/view {:style {:padding-bottom 8}}
    [react/i18n-text {:style styles/no-chats-text :key :chat-and-transact}]]
   [invite/invite-button]
   [react/view {:align-items :center :padding-top 8}
    [react/view {:style (styles/hr-wrapper)}]
    [react/i18n-text {:style (styles/or-text) :key :or}]]
   [react/view {:margin-top 16}
    [react/i18n-text {:style {:margin-horizontal 16
                              :text-align        :center}
                      :key   :follow-your-interests}]
    [react/view {:style styles/tags-wrapper}
     [react/view {:flex-direction :row :flex-wrap :wrap :justify-content :center}
      (for [chat (new-public-chat/featured-public-chats)]
        (new-public-chat/render-topic chat))]]]])

(defn welcome-blank-page []
  [react/view {:style {:flex 1 :flex-direction :row :align-items :center :justify-content :center}}
   [react/i18n-text {:style styles/welcome-blank-text :key :welcome-blank-message}]])

(defonce search-active? (reagent/atom false))

(defn search-input-wrapper [search-filter chats]
  [react/view {:padding-horizontal 16
               :padding-vertical   10}
   [search-input/search-input
    {:search-active? search-active?
     :search-filter  search-filter
     :on-cancel      #(re-frame/dispatch [:search/home-filter-changed nil])
     :on-blur        #(when-not (seq chats)
                        (re-frame/dispatch [:search/home-filter-changed nil]))
     :on-focus       (fn [search-filter]
                       (when-not search-filter
                         (re-frame/dispatch [:search/home-filter-changed ""])))
     :on-change      (fn [text]
                       (re-frame/dispatch [:search/home-filter-changed text]))}]])

(views/defview chats-list []
  (views/letsubs [loading? [:chats/loading?]
                  {:keys [chats search-filter]} [:home-items]
                  {:keys [hide-home-tooltip?]} [:multiaccount]]
    (if loading?
      [react/view {:flex 1 :align-items :center :justify-content :center}
       [react/activity-indicator {:animating true}]]
      (if (and (empty? chats)
               (empty? search-filter)
               hide-home-tooltip?
               (not @search-active?))
        [welcome-blank-page]
        [list/flat-list
         {:key-fn                         :chat-id
          :keyboard-should-persist-taps   :always
          :data                           chats
          :render-fn                      (fn [home-item] [inner-item/home-list-item home-item])
          :header                         (when (or (seq chats) @search-active?)
                                            [search-input-wrapper search-filter chats])
          :footer                         (if (and (not hide-home-tooltip?) (not @search-active?))
                                            [home-tooltip-view]
                                            [react/view {:height 68}])}]))))

(views/defview plus-button []
  (views/letsubs [logging-in? [:multiaccounts/login]]
    [react/view styles/action-button-container
     [quo/button {:type                :scale
                  :accessibility-label :new-chat-button
                  :on-press            (when-not logging-in?
                                         #(re-frame/dispatch [:bottom-sheet/show-sheet :add-new {}]))}
      [react/view (styles/action-button)
       (if logging-in?
         [react/activity-indicator {:color     colors/white-persist
                                    :animating true}]
         [icons/icon :main-icons/add {:color colors/white-persist}])]]]))

(defn home []
  [react/keyboard-avoiding-view {:style styles/home-container}
   [connectivity/connectivity
    [topbar/topbar {:title        :t/chat :navigation :none
                    :show-border? true}]
    [chats-list]]
   [plus-button]])
