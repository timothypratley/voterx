(ns voterx.main
  (:require
    [reagent.core :as reagent]
    [voterx.comm :as comm]
    [voterx.views.main :as main]
    [voterx.views.login :as login]))

(enable-console-print!)

(defn render []
  (reagent/render-component
    [main/main]
    (.getElementById js/document "container")))

(defn init []
  (comm/init)
  (login/init)
  (render))
