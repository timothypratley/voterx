(ns voterx.views.home
  (:require
    [voterx.db :as db]
    [voterx.firebase :as firebase]
    [voterx.views.login :as login]
    [voterx.views.d3 :as d3]
    [voterx.views.text-entry :as text-entry]
    [clojure.string :as string]
    [reagent.core :as reagent]
    [goog.crypt :as crypt])
  (:import
    [goog.crypt Md5]))

(defn gid2dbid [gid]
  (js/parseInt (second (re-matches #".*-(\d+)" gid))))

(defn gid2uid [gid]
  (second (re-matches #"(.*)-\d+" gid)))

(defn graph-view [conns]
  (let [nodes (db/nodes conns)
        edges (db/edges conns)
        selected-id (reagent/atom nil)
        editing (reagent/atom nil)]
    (fn a-graph-view []
      [d3/graph nodes edges selected-id editing
       {:shift-click-node
        (fn create-edge-or-remove-node [a b]
          (when-let [uid (:uid @firebase/user)]
            (when-let [conn (@conns uid)]
              (if (= a b)
                (db/retract-node conn (gid2dbid a))
                (if (= uid (gid2uid a) (gid2uid b))
                  (db/add-entity
                    conn
                    {:from (gid2dbid a)
                     :to (gid2dbid b)})
                  (if (= uid (gid2uid b))
                    (db/add-entities
                      conn
                      [(assoc (dissoc (first (filter #(= (:db/id %) a) @nodes)) :db/id :uid)
                         :db/id -1)
                       {:from -1
                        :to (gid2dbid b)}])
                    (if (= uid (gid2uid a))
                      (db/add-entities
                        conn
                        [(assoc (dissoc (first (filter #(= (:db/id %) b) @nodes)) :db/id :uid)
                           :db/id -1)
                         {:from (gid2dbid a)
                          :to -1}])
                      (db/add-entities
                        conn
                        [(assoc (dissoc (first (filter #(= (:db/id %) a) @nodes)) :db/id :uid)
                           :db/id -1)
                         (assoc (dissoc (first (filter #(= (:db/id %) b) @nodes)) :db/id :uid)
                           :db/id -2)
                         {:from -1
                          :to -2}])))))
              (firebase/save ["users" uid "db"] (pr-str @conn)))))
        :shift-click-edge
        (fn remove-edge [edge]
          (when-let [uid (:uid @firebase/user)]
            (when (= uid (:uid edge))
              (when-let [conn (@conns uid)]
                (db/retract conn (gid2dbid (:id edge)))
                (firebase/save ["users" uid "db"] (pr-str @conn))))))}])))

(defn md5-hash [s]
  (let [md5 (Md5.)]
    (.update md5 (string/trim s))
    (crypt/byteArrayToHex (.digest md5))))

(defn db-selector [conns on off]
  ;; TODO: how to avoid loading the "db" subpath?
  ;; TODO: expose the on set as part of firebase hof
  (let [selected (reagent/atom #{})]
    [firebase/on ["users"]
     (fn [users]
       [:ul
        (doall
          (for [[uid user] (js->clj @users)]
            ^{:key uid}
            [:li
             {:style {:display "inline-block"}}
             [:div
              {:style {:background-color (when (@selected uid)
                                           (d3/rgb (d3/color-for uid)))
                       :border (when (= uid (:uid @firebase/user)) "1px solid black")}}
              (let [photo-url (or (some-> user (get "settings") (get "photo-url"))
                                  (str "//www.gravatar.com/avatar/" (md5-hash uid) "?d=wavatar"))]
                [:div.mdl-button.mdl-button--fab.mdl-button--colored
                 {:title (or (some-> user (get "settings") (get "display-name")) uid)
                  :style {:background-image (str "url(" photo-url ")")
                          :background-size "cover"
                          :background-repeat "no-repeat"}
                  :on-click
                  (fn uid-selected [e]
                    (if (@selected uid)
                      (do
                        (swap! selected disj uid)
                        (off ["users" uid "db"]))
                      (do
                        (swap! selected conj uid)
                        (on ["users" uid "db"]))))}])
              (when
                [:strong "(my data)"])]]))])]))

(defn graph-edit []
  (let [conns (reagent/atom {})
        add-conn (fn add-conn [path x]
                   (db/add-conn conns (second path) x))
        clear-conn (fn clear-conn [path]
                     (swap! conns dissoc (second path)))]
    (fn []
      [firebase/with-refs-only add-conn clear-conn
       (fn home-component [on off]
         [:section
          [:p
           "Welcome! You need to be logged in and have checked yourself in the list of databases to edit your data.
           A text entry will appear where you can add new nodes.
           Click on a node, then shift click another node to link them.
           Click on a node, then shift click the same node to delete it.
           Your data saves whenever you change it."]
          [db-selector conns on off]
          [:div.mdl-grid
           [:div.mdl-cell.mdl-cell--8-col
            [graph-view conns]]
           (when-let [uid (:uid @firebase/user)]
             (when-let [conn (@conns uid)]
               [:div.mdl-cell.mdl-cell--4-col
                [text-entry/add-entity-form uid conn]
                [:div
                 [:p
                  "If something goes horribly wrong, you can start again by deleting all your data.
                  But hopefully you will never need to click on this."]
                 [:center
                  [:button
                   {:on-click
                    (fn [e]
                      (firebase/save ["users" uid "db"] nil))}
                   "Delete all my data"]]]]))]])])))

(defn home []
  [:div.mdl-layout.mdl-layout--fixed-header
   [:header.mdl-layout__header
    [:div.mdl-layout__header-row
     [login/login-view]]]
   [:main
    [:section
     [graph-edit]]]])
