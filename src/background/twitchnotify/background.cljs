(ns twitchnotify.background
  (:require-macros [chromex.support :refer [runonce]])
  (:require [twitchnotify.background.core :as core]))

(runonce
  (core/init!))