(ns voterx.views.home
  (:require
    [voterx.db :as db]
    [voterx.firebase :as firebase]
    [voterx.views.login :as login]
    [voterx.views.d3 :as d3]
    [voterx.views.text-entry :as text-entry]
    [reagent.core :as reagent]
    [clojure.string :as string]
    [goog.crypt :as crypt])
  (:import
    [goog.crypt Md5]))

(defn gid2dbid [gid]
  (js/parseInt (second (re-matches #".*-(\d+)" gid))))

(defn graph-view [conns]
  (let [nodes (db/nodes conns)
        edges (db/edges conns)
        selected-id (reagent/atom nil)
        editing (reagent/atom nil)
        callbacks {:shift-click-node
                   (fn create-edge-or-remove-node [a b]
                     (when-let [uid (:uid @firebase/user)]
                       (when-let [conn (@conns uid)]
                         (if (= a b)
                           (db/retract conn (gid2dbid a))
                           (db/add-entity
                             conn
                             {:from (gid2dbid a)
                              :to (gid2dbid b)}))
                         (firebase/save ["users" uid "db"] (pr-str @conn)))))
                   :shift-click-edge
                   (fn remove-edge [from mid to]
                     (prn "REMOVE" from mid to)
                     ;; TODO: get the entity id for mid!
                     #_(when-let [uid (:uid @firebase/user)]
                       (when-let [conn (@conns uid)]
                         (db/retract conn mid))))}]
    (fn a-graph-view []
      [d3/graph nodes edges selected-id editing callbacks])))

(defn navbar []
  [:div
   [:h1
    [:img {:src "brand.jpg"
           :style {:height "75px"}}]
    "Voter"
    [:span {:style {:font-family "cursive"}} "X"]]
   [login/login-view]
   [:p
    "Welcome! You need to be logged in and have checked yourself in the list of databases to edit your data.
    A text entry will appear where you can add new nodes.
    Click on a node, then shift click another node to link them.
    Click on a node, then shift click the same node to delete it.
    Your data saves whenever you change it.
    Linking between your data and other people's data is not yet supported."]])

(defn md5-hash [s]
  (let [md5 (Md5.)]
    (.update md5 (string/trim s))
    (crypt/byteArrayToHex (.digest md5))))

(defn db-selector [conns on off]
  ;; TODO: how to avoid loading the "db" subpath?
  [firebase/on ["users"]
   (fn [users]
     [:ul.mdl-list
      (doall
        (for [[uid user] (js->clj @users)]
          ^{:key uid}
          [:li.mdl-list__item
           {:style {:padding "0px"}}
           [:input.mdl-checkbox__input
            {:type "checkbox"
             :on-change
             (fn [e]
               (if (.. e -target -checked)
                 (on ["users" uid "db"])
                 (off ["users" uid "db"])))}]
           [:span {:style {:background-color (str "rgb(" (string/join "," (d3/color-for uid)) ")")}}
            (let [photo-url (or (some-> user (get "settings") (get "photo-url"))
                                (str "//www.gravatar.com/avatar/" (md5-hash uid) "?d=wavatar"))]
              [:button.mdl-button.mdl-js-button.mdl-button--fab.mdl-button--colored
               {:style {:background-image (str "url(" photo-url ")")
                        :background-size "cover"
                        :background-repeat "no-repeat"}}])]
           [:span (or (some-> user (get "settings") (get "display-name")) uid)]
           (when (= uid (:uid @firebase/user))
             [:strong "(my data)"])
           (when (@conns uid)
             [:strong "(loaded)"])]))])])

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
          [:div.mdl-grid
           [:div.mdl-cell.mdl-cell--8-col [graph-view conns]]
           (when-let [uid (:uid @firebase/user)]
             (when-let [conn (@conns uid)]
               [:div.mdl-cell.mdl-cell--4-col [text-entry/add-entity-form uid conn]]))]])])))
