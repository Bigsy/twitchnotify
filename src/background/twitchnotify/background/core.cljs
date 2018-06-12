(ns twitchnotify.background.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [twitchnotify.background.storage :as ms]
            [goog.string :as gstring]
            [goog.string.format]
            [cljs.core.async :refer [<! chan]]
            [chromex.logging :refer-macros [log info warn error group group-end]]
            [chromex.chrome-event-channel :refer [make-chrome-event-channel]]
            [chromex.protocols :refer [post-message! get-sender]]
            [chromex.ext.tabs :as tabs]
            [chromex.ext.runtime :as runtime]
            [chromex.ext.notifications :as not]
            [chromex.ext.browser-action :as ba]))

(def clients (atom []))
(def stream-state (atom {}))

(defn add-client! [client]
  (swap! clients conj client))

(defn remove-client! [client]
  (let [remove-item (fn [coll item] (remove #(identical? item %) coll))]
    (swap! clients remove-item client)))

(defn set-popup-icon []
  (go (let [val (<! (ms/pull "active"))]
        (if val
          (ba/set-icon #js {:path "images/twitch24gc.png"})
          (ba/set-icon #js {:path "images/twitch24rb.png"})))))

(defn enable-disable []
  (go (let [val (<! (ms/pull "active"))]
        (if val
          (do (ms/add "active" false)
              (set-popup-icon))
          (do (ms/add "active" true)
              (set-popup-icon))))))

(defn game-changed? [current-state game-name]
  (not= game-name (:game-name current-state)))

(defn notify? [{:keys [url], {:keys [id]} :tab} message]

  (let [current-state @stream-state
        game-name     (:game-name message)]
    (if-let [tab ((keyword url) current-state)]
      (if (game-changed? tab game-name)
        (do
          (swap! stream-state (fn [x]
                                (merge x {(keyword url) {:id           id
                                                         :streamer-img ""
                                                         :game-name    game-name}}))) true) false)
      (do (swap! stream-state (fn [x]
                                (merge x {(keyword url) {:id           id
                                                         :streamer-img ""
                                                         :game-name    game-name}}))) false))))

(defn run-client-message-loop! [client]
  (go-loop []
    (when-some [message (js->clj (<! client) :keywordize-keys true)]
      (let [sender (js->clj (get-sender client) :keywordize-keys true)
            enabled? (<! (ms/pull "active"))
            message-type (:message-type message)
            game-name (:game-name message)
            stream-img (:stream-img message)
            stream-name (:stream-name message)]
        (log "message: " message)
        (log "sender:" sender)
        (log "enabled?:" enabled?)
        (log "img:" stream-img)
        (log "name:" stream-name)




        (if (and (= message-type "game-update") (notify? sender message) enabled?)
          (not/create "2" (clj->js {:type           "basic"
                                    :title          (str stream-name " now playing:")
                                    :message        game-name
                                    :contextMessage ""
                                    :iconUrl        stream-img}))))
      (recur))
    (remove-client! client)))

(defn handle-client-connection! [client]
  (add-client! client)
  (run-client-message-loop! client))

(defn process-chrome-event [event-num event]
  (let [[event-id event-args] event]
    (case event-id
      ::runtime/on-connect (apply handle-client-connection! event-args)
      ::ba/on-clicked (enable-disable)
      nil)))

(defn run-chrome-event-loop! [chrome-event-channel]
  (go-loop [event-num 1]
    (when-some [event (<! chrome-event-channel)]
      (process-chrome-event event-num event)
      (recur (inc event-num)))))

(defn boot-chrome-event-loop! []
  (let [chrome-event-channel (make-chrome-event-channel (chan))]
    (tabs/tap-all-events chrome-event-channel)
    (ba/tap-all-events chrome-event-channel)
    (runtime/tap-all-events chrome-event-channel)
    (run-chrome-event-loop! chrome-event-channel)))

(defn init! []
  (boot-chrome-event-loop!)
  (set-popup-icon))


;--------------
(comment
  (let [data1      {:url "wibble1" :tab {:id 1}}
        data1-1    {:url "wibble1" :tab {:id 1}}
        data2      {:url "wibble2" :tab {:id 2}}

        message1   {:game-name "1"}
        message1-1 {:game-name "2"}
        message2   {:game-name "1"}]
    (reset! stream-state {})
    (assert (false? (notify? data1 message1)))
    (assert (notify? data1-1 message1-1))
    (assert (false? (notify? data1-1 message1-1)))
    (assert (false? (notify? data2 message2)))
    (assert (false? (notify? data2 message2)))))

