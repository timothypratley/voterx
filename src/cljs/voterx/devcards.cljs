(ns voterx.devcards
  (:require [devcards.core :as dc])
  (:require-macros [devcards.core :refer [start-devcard-ui!]]))

(defn init []
  (start-devcard-ui!))