(ns status-im.ui.screens.chat.message.reactions-picker
  (:require [cljs-bean.core :as bean]
            [status-im.ui.screens.chat.message.styles :as styles]
            [status-im.constants :as constants]
            [reagent.core :as reagent]
            [quo.react-native :as rn]
            [quo.react :as react]
            [quo.animated :as animated]
            [quo.components.safe-area :as safe-area]
            [quo.core :as quo]))

(def tabbar-height 36)
(def text-input-height 54)

(def animation-duration 150)

(def scale         0.8)
(def translate-x   27)
(def translate-y   -24)

(defn picker [{:keys [outgoing actions on-close send-emoji]}]
  [animated/view {:style (styles/container-style {:outgoing outgoing})}
   [rn/view {:style (styles/reactions-picker-row)}
    (for [[id resource] constants/reactions]
      ^{:key id}
      [rn/touchable-opacity {:on-press #(send-emoji id)
                             :style    {:padding-vertical   6
                                        :padding-horizontal 5}}
       [rn/image {:source resource
                  :style  {:height 32
                           :width  32}}]])]
   [rn/view {:style (styles/quick-actions-row)}
    (for [action actions
          :let   [{:keys [label on-press]} (bean/bean action)]]
      ^{:key label}
      [quo/button {:type     :secondary
                   :on-press (fn []
                               (on-close)
                               (js/setTimeout on-press animation-duration))}
       label])]])

(def modal
  (reagent/adapt-react-class
   (fn [props]
     (let [{outgoing       :outgoing
            animation      :animation
            spring         :spring
            top            :top
            message-height :messageHeight
            display-photo  :displayPhoto
            on-close       :onClose
            actions        :actions
            send-emoji     :sendEmoji
            children       :children}
           (bean/bean props)
           {bottom-inset :bottom}  (safe-area/use-safe-area)
           {window-height :height} (rn/use-window-dimensions)

           {picker-height    :height
            on-picker-layout :on-layout} (rn/use-layout)

           full-height   (+ message-height picker-height top)
           max-height    (- window-height bottom-inset tabbar-height text-input-height)
           translate-top (- (max 0 (- full-height max-height)))
           translation-x (if outgoing
                           translate-x
                           (* -1 translate-x))]
       (reagent/as-element
        [:<>
         [rn/view {:style {:position :absolute
                           :flex     1
                           :top      0
                           :bottom   0
                           :left     0
                           :right    0}}
          [rn/touchable-without-feedback
           {:on-press on-close}
           [animated/view {:style {:flex             1
                                   :opacity          animation
                                   :background-color "rgba(0,0,0,0.5)"}}]]]
         [animated/view {:pointer-events :box-none
                         :style          {:top       top
                                          :opacity   animation
                                          :transform [{:translateY (animated/mix animation 0 translate-top)}]
                                          :postion   :absolute}}
          (into [:<>] (react/get-children children))
          [animated/view {:on-layout      on-picker-layout
                          :pointer-events :box-none
                          :style          (merge (styles/picker-wrapper-style {:display-photo? display-photo
                                                                               :outgoing       outgoing})
                                                 {:top       message-height
                                                  :opacity   animation
                                                  :transform [{:translateX (animated/mix spring translation-x 0)}
                                                              {:translateY (animated/mix spring translate-y 0)}
                                                              {:scale (animated/mix spring scale 1)}]})}
           [picker {:outgoing   outgoing
                    :actions    actions
                    :on-close   on-close
                    :send-emoji send-emoji
                    :animation  animation}]]]])))))
