(ns voterx.views.main
  (:require
    [goog.dom.forms :as forms]
    [voterx.db :as db]
    [voterx.comm :as comm]
    [voterx.views.d3 :as d3]))

(defn add-entity-form []
  [:form
   {:on-submit
    (fn edit [e]
      (.preventDefault e)
      (db/add-entity
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
        edges (db/edges)]
    (fn []
      (prn "zdsafsda" @nodes)
      [d3/graph nodes edges])))

(defn toolbar []
  [:center
   [:button.mdl-button.mdl-js-button.mdl-button--raised.mdl-button--accent
    {:on-click
     (fn save-click [e]
       (comm/save-db))}
    "Save"]
   [:button.mdl-button.mdl-js-button.mdl-button--raised.mdl-button--accent
    {:on-click
     (fn load-click [e]
       (comm/load-db))}
    "Load"]])

(defn main []
  (let [fun (db/fun)]
    (fn []
      [:div
       [:div.mdl-grid
        [:div.mdl-cell.mdl-cell--12-col (pr-str @fun)]
        [:div.mdl-cell.mdl-cell--8-col [graph-view]]
        [:div.mdl-cell.mdl-cell--4-col [add-entity-form]]]
       [toolbar]])))
