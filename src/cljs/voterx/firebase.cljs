(ns voterx.firebase
  (:require
    [cljsjs.firebase]
    [cljs.pprint :refer [pprint]]
    [cljs.core.async :refer [chan put! <!]]
    [clojure.string :as string]
    [voterx.db :as db]
    [reagent.core :as reagent]
    [reagent.ratom :as ratom]
    [cljs.test]
    [devcards.core]
    [goog.dom.forms :as forms])
  (:require-macros
    [devcards.core :refer [defcard-rg]]))

(defonce user
  (reagent/atom nil))

(defonce db-list
  (reagent/atom []))

(defn db-ref [path]
  (.ref (js/firebase.database) (string/join "/" path)))

(defn init []
  (js/firebase.initializeApp
    #js {:apiKey "AIzaSyDosF04KpvPslT5g0mzjZ0Q-paRluRWC-M"
         :authDomain "voterx-e88a1.firebaseapp.com"
         :databaseURL "https://voterx-e88a1.firebaseio.com"
         :storageBucket "voterx-e88a1.appspot.com"})
  (.onAuthStateChanged
    (js/firebase.auth)
    (fn auth-state-changed [user-obj]
      ;; TODO: better way of cljsizing user-obj?
      (reset! user {:photoURL (.-photoURL user-obj)
                    :displayName (.-displayName user-obj)
                    :uid (.-uid user-obj)}))
    (fn auth-error [error]
      (js/console.log error)))
  (.on
    ;; TODO: how do I only get the keys, not the full objects??
    (db-ref ["users"])
    "value"
    (fn received-dbs [snapshot]
      (let [dbs (js->clj (.val snapshot))]
        (reset! db-list dbs)))))

(defn save [path value]
  (.set (db-ref path) value))

(defcard-rg save-card
  [:form
   {:on-submit
    (fn [e]
      (.preventDefault e)
      (save ["test"] (forms/getValueByName (.-target e) "test")))}
   [:div "Type a message and save it to Firebase"]
   [:input
    {:type "text"
     :name "test"}]
   [:input
    {:type "submit"}]])

(defn once [path]
  (let [a (reagent/atom nil)]
    (.once
      (db-ref path)
      "value"
      (fn received-db [snapshot]
        (reset! a (.val snapshot))))
    a))

(defcard-rg once-card
  (fn []
    (let [a (once ["test"])]
      (fn []
        [:div @a]))))

(defn on [path f]
  (let [ref (db-ref path)
        a (reagent/atom nil)]
    (.on ref "value" (fn [x]
                       (reset! a (.val x))))
    (reagent/create-class
      {:display-name "listener"
       :component-will-unmount
       (fn will-unmount-listener [this]
         (.off ref))
       :reagent-render
       (fn render-listener [args]
         (into [f a] args))})))

(defcard-rg on-card
  [on ["test"]
   (fn [a]
     [:div @a])])

(defn sign-in-with-popup []
  (.signInWithPopup
    (js/firebase.auth.)
    (js/firebase.auth.GoogleAuthProvider.)))

(defn logout []
  ;; TODO: add then/error handlers
  (.signOut (js/firebase.auth))
  (reset! user nil))

(defn save-db [uid]
  (.set
    (db-ref ["users" uid "db"])
    (pr-str @(or (@db/conns uid)
                 (db/init uid)))))

(defn load-db [uid]
  (.on
    (db-ref ["users" uid "db"])
    "value"
    (fn received-db [snapshot]
      (let [db (.val snapshot)]
        (db/add-conn uid db)))))
