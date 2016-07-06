(ns voterx.comm
  (:require
    [cljs.pprint :refer [pprint]]
    [cljs.core.async :refer [chan put! <!]]
    [cljs.tools.reader.edn :as edn]
    [cljsjs.firebase]
    [matchbox.core :as m]
    [voterx.db :as db]
    [datascript.core :as d])
  (:require-macros
    [cljs.core.async.macros :refer [go-loop]]))

(enable-console-print!)

(def base-uri "https://voterx-e88a1.firebaseio.com/")

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
         :storageBucket "voterx-e88a1.appspot.com"}))

(def root (m/connect base-uri))
(m/auth-anon root)
(safe-prn "AUTH:" (m/auth-info root))

(def my-db
  (m/get-in root [:users "NJmZuBej2NcbsXq167MjjoDo8Bf1" :db]))

(defn save-db []
  (m/reset! my-db (pr-str @db/conn)))

(defn load-db []
  (m/deref my-db (fn received-db [db]
                   (safe-prn "DB:" db)
                   (db/reset-conn! (edn/read-string {:readers d/data-readers} db)))))
