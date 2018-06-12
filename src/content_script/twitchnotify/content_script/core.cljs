(ns twitchnotify.content-script.core
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require [cljs.core.async :refer [<!]]
            [chromex.logging :refer-macros [log info warn error group group-end]]
            [chromex.protocols :refer [post-message!]]
            [chromex.ext.runtime :as runtime :refer-macros [connect]]))

; -- a message loop ---------------------------------------------------------------------------------------------------------

(defn process-message! [message]
  (log "CONTENT SCRIPT: got message:" message))

(defn run-message-loop! [message-channel]
  (log "CONTENT SCRIPT: starting message loop...")
  (go-loop []
    (when-some [message (<! message-channel)]
      (process-message! message)
      (recur))
    (log "CONTENT SCRIPT: leaving message loop")))

; -- a simple page analysis  ------------------------------------------------------------------------------------------------

(def TIMEOUT 5000)


(defn polling-request
  [handler timeout]
  ;; Will call the handler every timeout interval
  (js/setInterval handler timeout))

(defn send-poll [background-port]
  (polling-request
    (fn []

      (let [game-name   (.-innerText (first (array-seq (.querySelectorAll js/document "[data-a-target=\"stream-game-link\"]"))))
            img-link    (.-src (first (array-seq (.getElementsByClassName js/document "qa-broadcaster-logo"))))
            stream-name (.-innerText (first (array-seq (.getElementsByClassName js/document "player-text-link player-text-link--no-color qa-display-name"))))]

        ;(log "WIB " script-elements)

        (post-message! background-port #js {:message-type "game-update"
                                            :game-name    (str game-name)
                                            :stream-img   img-link
                                            :stream-name  stream-name})))


    TIMEOUT))

(defn connect-to-background-page! []
  (let [background-port (runtime/connect)]
    (post-message! background-port "hello from CONTENT SCRIPT!")
    (run-message-loop! background-port)
    (send-poll background-port)))

; -- main entry point -------------------------------------------------------------------------------------------------------

(defn init! []
  (log "CONTENT SCRIPT: init")
  (connect-to-background-page!))
