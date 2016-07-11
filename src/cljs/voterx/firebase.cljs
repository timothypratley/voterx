(ns voterx.firebase
  (:require
    [cljs.pprint :refer [pprint]]
    [cljs.core.async :refer [chan put! <!]]
    [clojure.string :as string]
    [voterx.db :as db]
    [cljs.tools.reader.edn :as edn]
    [datascript.core :as d]
    [reagent.core :as reagent])
  (:require-macros
    [cljs.core.async.macros :refer [go-loop]]))

(defonce user
  (reagent/atom nil))

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
      (js/console.log error))))

(defn sign-in-with-popup []
  (.signInWithPopup
    (js/firebase.auth.)
    (js/firebase.auth.GoogleAuthProvider.)))

(defn logout []
  ;; TODO: add then/error handlers
  (.signOut (js/firebase.auth))
  (reset! user nil))

(defn db-ref [& path]
  (.ref (js/firebase.database) (string/join "/" path)))

(defn save-db []
  (when-let [uid (:uid @user)]
    (.set
      (db-ref "users" uid "db")
      (pr-str @db/conn))))

(defn load-db []
  (when-let [uid (:uid @user)]
    (.on
      (db-ref "users" uid "db")
      "value"
      (fn received-db [snapshot]
        (let [db (.val snapshot)]
          (safe-prn "DB:" db)
          (db/reset-conn! (edn/read-string {:readers d/data-readers} db)))))))
