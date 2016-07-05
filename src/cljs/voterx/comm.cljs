(ns voterx.comm
  (:require
    [cljs.pprint :refer [pprint]]
    [cljs.core.async :refer [chan put! <!]]
    [cljs.tools.reader.edn :as edn]
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

(def root (m/connect base-uri))
(m/auth-anon root)
;;(m/auth-with-oauth-popup r )
(safe-prn "AUTH:" (m/auth-info root))

(def my-db (m/get-in root [:users "NJmZuBej2NcbsXq167MjjoDo8Bf1" :db]))

(defn save-db []
  (m/reset! my-db (doto (pr-str @db/conn)
                    (prn "FFFFF"))))

(defn load-db []
  (m/deref my-db (fn received-db [db]
                   (safe-prn "DB:" db)
                   (db/reset-conn! (edn/read-string {:readers d/data-readers} db)))))
