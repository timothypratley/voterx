(ns voterx.main
  (:require
    [reagent.core :as reagent]
    [voterx.firebase :as firebase]
    [voterx.views.main :as main]))

(enable-console-print!)

(defn render []
  (reagent/render-component
    [main/main]
    (.getElementById js/document "container")))

(defn init []
  (firebase/init)
  (render))
