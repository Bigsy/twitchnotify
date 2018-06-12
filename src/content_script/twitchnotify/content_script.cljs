(ns twitchnotify.content-script
  (:require-macros [chromex.support :refer [runonce]])
  (:require [twitchnotify.content-script.core :as core]))

(runonce
  (core/init!))
