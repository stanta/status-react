(ns status-im.ui.screens.chat.utils
  (:require [status-im.ethereum.stateofus :as stateofus]
            [status-im.i18n :as i18n]
            [status-im.ui.components.react :as react]
            [status-im.ui.components.colors :as colors]))

(def ^:private reply-symbol "↪ ")

(defn format-author
  ([contact-name] (format-author contact-name false))
  ([contact-name modal]
   (if (= (aget contact-name 0) "@")
     (let [trimmed-name (subs contact-name 0 81)]
       [react/text {:number-of-lines 2
                    :style           {:color       (if modal colors/white colors/blue)
                                      :font-size   13
                                      :line-height 18
                                      :font-weight "500"}}
        (or (stateofus/username trimmed-name) trimmed-name)])
     [react/text {:style {:color       (if modal colors/white colors/gray)
                          :font-size   12
                          :line-height 18
                          :font-weight "400"}}
      contact-name])))

(defn format-reply-author [from username current-public-key style]
  (let [contact-name (str reply-symbol username)]
    (or (and (= from current-public-key)
             [react/text {:style (style true)}
              (str reply-symbol (i18n/label :t/You))])
        (if (or (= (aget contact-name 0) "@")
               ;; in case of replies
                (= (aget contact-name 1) "@"))
          (let [trimmed-name (subs contact-name 0 81)]
            [react/text {:number-of-lines 2
                         :style           (merge {:color       colors/blue
                                                  :font-size   13
                                                  :line-height 18
                                                  :font-weight "500"})}
             (or (stateofus/username trimmed-name) trimmed-name)])
          [react/text {:style (merge {:color       colors/gray
                                      :font-size   12
                                      :line-height 18
                                      :font-weight "400"})}
           contact-name]))))
