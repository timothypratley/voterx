(ns voterx.firebase
  (:require
    [cljs.pprint :refer [pprint]]
    [cljs.core.async :refer [chan put! <!]]
    [clojure.string :as string]
    [voterx.db :as db]
    [reagent.core :as reagent]
    [reagent.ratom :as ratom])
  (:require-macros
    [cljs.core.async.macros :refer [go-loop]]))

(defonce user
  (reagent/atom nil))

(defonce db-list
  (reagent/atom []))

(def prn-chan (chan))

(defn safe-prn [& msgs]
  (put! prn-chan msgs))

(go-loop []
  (let [msgs (<! prn-chan)]
    (doseq [msg msgs]
      (if (string? msg)
        (println msg)
        (pprint msg)))
    (println)
    (recur)))

(defn db-ref [path]
  (.ref (js/firebase.database) (string/join "/" path)))

(defn save [path value]
  (.set (db-ref path) value))

(defn load [uid]
  (.on
    (db-ref ["users" uid "db"])
    "value"
    (fn received-db [snapshot]
      (prn "GOT" (.val snapshot)))))

(defn listener [path f]
  (let [ref (db-ref path)
        a (reagent/atom nil)]
    (.on ref "value" (fn [x] (reset! a (.val x))))
    (reagent/create-class
      {:display-name "listener"
       :component-will-unmount
       (fn [this]
         (.off ref))
       :reagent-render
       (fn [args]
         [apply f a args])})))

(defn listen [path]
  (let [a (reagent/atom nil)
        r (db-ref path)]
    (.on
      r
      "value"
      (fn [snapshot]
        (prn "GOT" path)
        (reset! a (.val snapshot))))
    (ratom/add-on-dispose! a (fn [] (.off r)))
    a))

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
