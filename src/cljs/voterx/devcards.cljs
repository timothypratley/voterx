(ns voterx.devcards
  (:require
    [devcards.core]
    [voterx.main]
    [voterx.firebase :as firebase])
  (:require-macros
    [devcards.core :refer [start-devcard-ui! defcard-rg]]))

(enable-console-print!)

(defonce firebase (firebase/init))

(defn init []
  (start-devcard-ui!))
