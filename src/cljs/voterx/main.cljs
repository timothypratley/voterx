(ns voterx.main
  (:require
    [reagent.core :as reagent]
    [voterx.firebase :as firebase]
    [voterx.views.main :as main]
    [devcards.core])
  (:require-macros
    [devcards.core :refer [defcard]]))

(enable-console-print!)

(defn render []
  (when-let [element (.getElementById js/document "container")]
    (reagent/render-component
      [main/main]
      element)))

(defn init []
  (firebase/init)
  (render))

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
