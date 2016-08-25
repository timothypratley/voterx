(ns voterx.main
  (:require
    [reagent.core :as reagent]
    [voterx.firebase :as firebase]
    [voterx.views.home :as main]
    [devcards.core]
    [goog.events :as events]
    [goog.history.EventType :as EventType])
  (:require-macros
    [devcards.core :refer [defcard]])
  (:import
    [goog History]))

(defcard
  "### -- Agenda --
  #### 1. Write to and read from Firebase
  #### 2. Listen to change notifications
  #### 3. Add user authentication
  #### 4. Build a UI to make use of the data
  #### 5. Deploy the project
  #### 6. Examine the impact of optimized compilation
       * Deploy with whitespace optimization
       * Use or provide externs
       * Follow CLJSJS as a blueprint
  #### 7. Review JavaScript interop")

(defonce app-state (reagent/atom {}))

(enable-console-print!)

(defn render []
  (when-let [element (.getElementById js/document "container")]
    (reagent/render-component
      [main/home app-state]
      element)))

(defn navigation [event]
  (swap! app-state assoc :route (.-token event)))

(defn init []
  (doto (History.)
    (events/listen EventType/NAVIGATE navigation)
    (.setEnabled true))
  (firebase/init)
  (render))
