(ns voterx.views.main
  (:require
    [goog.dom.forms :as forms]
    [voterx.db :as db]
    [voterx.firebase :as firebase]
    [voterx.views.login :as login]
    [voterx.views.d3 :as d3]
    [reagent.core :as reagent]
    [clojure.string :as string]))

(defn add-entity-form []
  [:form
   {:on-submit
    (fn edit [e]
      (.preventDefault e)
      (db/add-entity
        (:uid @firebase/user)
        (into {}
              (for [[k v] (js->clj (.toObject (forms/getFormDataMap (.-target e))))]
                [(keyword k)
                 (if (<= (count v) 1)
                   (first v)
                   v)]))))}
   [:div#name.mdl-textfield.mdl-js-textfield
    {:style {:width "100%"}}
    [:input.mdl-textfield__input.mdl-js-textfield
     {:type "text"
      :name "name"}]
    [:label.mdl-textfield__label
     {:for "name"}
     "Title..."]]
   [:div#text.mdl-textfield.mdl-js-textfield.mdl-textfield--expandable
    {:style {:width "100%"}}
    [:textarea.mdl-textfield__input
     {:type "text"
      :name "text"
      :rows 5}]
    [:label.mdl-textfield__label
     {:for "text"}
     "Text..."]]
   [:center
    [:input.mdl-button.mdl-js-button--raised.mdl-button--accent
     {:type "submit"}]]])

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

(defn whacky []
  (prn (:uid @firebase/user) "***")
  (firebase/listener
    ["users" (:uid @firebase/user) "some-num"]
    (fn [a]
      [:h1 "Whack2" @a])))

(defn main []
  (let [x (reagent/atom true)]
    (fn a-main []
      [:div
       (when (and (:uid @firebase/user) @x)
         [whacky])
       [:button {:on-click (fn [e] (swap! x not))} "toggle"]
       [:button
        {:on-click
         (fn [e]
           (firebase/save ["users" (:uid @firebase/user) "some-num"] (rand-int 100)))}
        "save-rand"]
       [navbar]
       [db-selector]
       [:div.mdl-grid
        [:div.mdl-cell.mdl-cell--8-col [graph-view]]
        [:div.mdl-cell.mdl-cell--4-col [add-entity-form]]]
       [toolbar]])))
