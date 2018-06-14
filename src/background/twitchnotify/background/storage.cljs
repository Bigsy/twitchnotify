(ns twitchnotify.background.storage
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.core.async :refer [<! chan]]
            [chromex.logging :refer-macros [log info warn error group group-end]]
            [chromex.protocols :as ptcl]
            [chromex.ext.storage :as storage]))

(defn add [k v]
  (let [local-storage (storage/get-local)]
    (ptcl/set local-storage (clj->js {k v}))))

(defn pull [k]
  (go (let [local-storage (storage/get-local)
            [[items] error] (<! (ptcl/get local-storage k))]
        (if error
          (error "fetch " k " error:" error)
          (get (js->clj items) k)))))