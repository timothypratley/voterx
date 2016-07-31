(ns voterx.views.home
  (:require
    [voterx.db :as db]
    [voterx.firebase :as firebase]
    [voterx.views.login :as login]
    [voterx.views.d3 :as d3]
    [voterx.views.text-entry :as text-entry]
    [reagent.core :as reagent]
    [clojure.string :as string]))

(defn gid2dbid [gid]
  (js/parseInt (second (re-matches #".*-(\d+)" gid))))


(defn graph-view [conns]
  (let [nodes (db/nodes conns)
        edges (db/edges conns)
        selected-id (reagent/atom nil)
        editing (reagent/atom nil)
        callbacks {:add-edge
                   (fn add-edge [[a b]]
                     (prn "ADD" a b)
                     (when-let [uid (:uid @firebase/user)]
                       (when-let [conn (@conns uid)]
                         (db/add-entity
                           conn
                           {:from (gid2dbid a)
                            :to (gid2dbid b)})
                         (firebase/save ["users" uid "db"] (pr-str @conn)))))
                   :remove-node
                   (fn remove-node [id]
                     (when-let [uid (:uid @firebase/user)]
                       (when-let [conn (@conns uid)]
                         (db/retract conn id))))
                   :node-shape
                   (fn node-shape [id]
                     (prn "reshape" id))
                   :edge-weight
                   (fn edge-weight [id]
                     (prn "edge-weight"))}]
    (fn a-graph-view []
      [d3/graph nodes edges selected-id editing callbacks])))

(defn navbar []
  [:div
   [:h1
    [:img {:src "brand.jpg"
           :style {:height "75px"}}]
    "Voter"
    [:span {:style {:font-family "cursive"}} "X"]]
   [login/login-view]])

(defn db-selector [conns on off]
  (into
    [:ul.mdl-list]
    (for [user (keys @firebase/db-list)]
      [:li.mdl-list__item
       [:input.mdl-checkbox__input
        {:type "checkbox"
         :on-change
         (fn [e]
           (if (.. e -target -checked)
             (on ["users" user "db"])
             (off ["users" user "db"])))}]
       [:span {:style {:background-color (str "rgb(" (string/join "," (d3/color-for user)) ")")}} user]
       (when (= user (:uid @firebase/user))
         [:strong "My data "])
       (when (@conns user)
         [:strong "loaded "])])))

(defn home []
  (let [conns (reagent/atom {})
        add-conn (fn add-conn [path x]
                   (db/add-conn conns (second path) x))
        clear-conn (fn clear-conn [path]
                     (swap! conns dissoc (second path)))]
    (fn []
      [firebase/with-refs-only add-conn clear-conn
       (fn home-component [on off]
         [:div
          [navbar]
          [db-selector conns on off]
          [:div (pr-str (keys @conns))]
          [:div.mdl-grid
           [:div.mdl-cell.mdl-cell--8-col [graph-view conns]]
           (when-let [uid (:uid @firebase/user)]
             (when-let [conn (@conns uid)]
               [:div.mdl-cell.mdl-cell--4-col [text-entry/add-entity-form uid conn]]))]])])))
