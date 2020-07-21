(ns status-im.utils.config
  (:require [clojure.string :as string]
            ["react-native-config" :default react-native-config]))

(def config
  (memoize
   (fn []
     (js->clj react-native-config :keywordize-keys true))))

(defn get-config
  ([k] (get (config) k))
  ([k not-found] (get (config) k not-found)))

;; TODO(oskarth): Extend this to deal with true/false for Jenkins parameter builds
(defn enabled? [v] (= "1" v))

;; NOTE(oskarth): Feature flag deprecation lifecycles. We want to make sure
;; flags stay up to date and are removed once behavior introduced is stable.

(goog-define INFURA_TOKEN "40ec14d9d9384d52b7fbcfecdde4e2c0")

(def bootnodes-settings-enabled? (enabled? (get-config :BOOTNODES_SETTINGS_ENABLED "1")))
(def rpc-networks-only? (enabled? (get-config :RPC_NETWORKS_ONLY "1")))
(def mailserver-confirmations-enabled? (enabled? (get-config :MAILSERVER_CONFIRMATIONS_ENABLED)))
(def pairing-popup-disabled? (enabled? (get-config :PAIRING_POPUP_DISABLED "0")))
(def cached-webviews-enabled? (enabled? (get-config :CACHED_WEBVIEWS_ENABLED 0)))
(def snoopy-enabled? (enabled? (get-config :SNOOPY 0)))
(def dev-build? (enabled? (get-config :DEV_BUILD 0)))
(def erc20-contract-warnings-enabled? (enabled? (get-config :ERC20_CONTRACT_WARNINGS)))
(def tr-to-talk-enabled? (enabled? (get-config :TRIBUTE_TO_TALK 0)))
(def max-message-delivery-attempts (js/parseInt (get-config :MAX_MESSAGE_DELIVERY_ATTEMPTS "6")))
;; NOTE: only disabled in releases
(def local-notifications? (enabled? (get-config :LOCAL_NOTIFICATIONS "1")))
(def blank-preview? (enabled? (get-config :BLANK_PREVIEW "1")))
(def group-chat-enabled? (enabled? (get-config :GROUP_CHATS_ENABLED "0")))
(def tooltip-events? (enabled? (get-config :TOOLTIP_EVENTS "0")))
(def nimbus-enabled? (enabled? (get-config :STATUS_GO_ENABLE_NIMBUS "0")))
(def waku-enabled? (enabled? (get-config :WAKU_ENABLED "0")))
(def commands-enabled? (enabled? (get-config :COMMANDS_ENABLED "0")))
(def keycard-test-menu-enabled? (enabled? (get-config :KEYCARD_TEST_MENU "0")))
(def qr-test-menu-enabled? (enabled? (get-config :QR_READ_TEST_MENU "0")))
(def referrals-invite-enabled? (enabled? (get-config :ENABLE_REFERRAL_INVITE "0")))

;; CONFIG VALUES
(def log-level
  (string/upper-case (get-config :LOG_LEVEL "")))
(def fleet (get-config :FLEET "eth.staging"))
(def default-network (get-config :DEFAULT_NETWORK))
(def pow-target (js/parseFloat (get-config :POW_TARGET "0.0001")))
(def pow-time (js/parseInt (get-config :POW_TIME "1")))
(def max-installations 2)
