(ns voterx.views.home
  (:require
    [voterx.db :as db]
    [voterx.firebase :as firebase]
    [voterx.views.login :as login]
    [voterx.views.d3 :as d3]
    [voterx.views.text-entry :as text-entry]
    [reagent.core :as reagent]
    [clojure.string :as string]))

(defn graph-view []
  (let [nodes (db/nodes)
        edges (db/edges)
        selected-id (reagent/atom nil)
        editing (reagent/atom nil)
        root (reagent/atom {})]
    (fn a-graph-view []
      [d3/graph nodes edges selected-id editing root])))

(defn toolbar []
  [:center
   (if-let [uid (:uid @firebase/user)]
     [:button.mdl-button.mdl-js-button.mdl-button--raised.mdl-button--accent
      {:on-click
       (fn save-click [e]
         (firebase/save-db uid))}
      "Save"]
     "Must be logged in to save.")])

(defn navbar []
  [:div
   [:h1
    [:img {:src "brand.jpg"
           :style {:height "75px"}}]
    "Voter"
    [:span {:style {:font-family "cursive"}} "X"]]
   [login/login-view]])

(defn db-selector []
  (into
    [:ul]
    (for [user (keys @firebase/db-list)]
      [:li
       [:span {:style {:background-color (str "rgb(" (string/join "," (d3/color-for user)) ")")}} user]
       (when (= user (:uid @firebase/user))
         [:strong " My data "])
       (when (@db/conns user)
         [:strong " loaded "])
       [:button.mdl-button.mdl-js-button.mdl-button--raised.mdl-button--accent
        {:on-click
         (fn load-click [e]
           (firebase/load-db user))}
        "Load"]])))

(defn home []
  [:div
   [:button
    {:on-click
     (fn [e]
       (firebase/save ["users" (:uid @firebase/user) "some-num"] (rand-int 100)))}
    "save-rand"]
   [navbar]
   [db-selector]
   [:div.mdl-grid
    [:div.mdl-cell.mdl-cell--8-col [graph-view]]
    [:div.mdl-cell.mdl-cell--4-col [text-entry/add-entity-form]]]
   [toolbar]])
