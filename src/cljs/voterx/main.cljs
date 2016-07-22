(ns voterx.main
  (:require
    [reagent.core :as reagent]
    [voterx.firebase :as firebase]
    [voterx.views.main :as main]))

(enable-console-print!)

(defn render []
  (when-let [element (.getElementById js/document "container")]
    (reagent/render-component
      [main/main]
      element)))

(defn init []
  (firebase/init)
  (render))
