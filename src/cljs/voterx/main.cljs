(ns voterx.main
  (:require
    [reagent.core :as reagent]
    [voterx.comm :as comm]
    [voterx.views.main :as main]))

(enable-console-print!)

(defn init []
  (reagent/render-component
    [main/main]
    (.getElementById js/document "container")))
