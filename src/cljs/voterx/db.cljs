(ns voterx.db
  (:require
    [reagent.core :as reagent]
    [posh.reagent :refer [pull q posh! transact!]]
    [datascript.core :as d])
  (:require-macros
    [reagent.ratom :refer [reaction]]))

(def schema
  {:to {:db/cardinality :db.cardinality/many}})

(def conn
  (d/create-conn schema))

(defn reset-conn! [db]
  (d/reset-conn! conn db))

(transact!
  conn
  [{:db/id -1
    :name "Tim"}
   {:db/id -2
    :name "Time"}
   {:db/id -3
    :name "Think"}
   {:db/id -4
    :name "Learn"}
   {:db/id -5
    :name "Do"}
   {:db/id -6
    :name "Improve"}
   {:db/id -7
    :name "Meta"}
   {:db/id -11
    :from 1
    :to 2}
   {:db/id -12
    :from 2
    :to 3}
   {:db/id -13
    :from 2
    :to 4}
   {:db/id -14
    :from 2
    :to 5}
   {:db/id -15
    :from 1
    :to 6}
   {:db/id -16
    :from 6
    :to 7}])

(defn connect []
  (posh! conn))

(connect)

(defn fun []
    (q '[:find ?e
         :where [?e _ _]]
       conn))

#_(defn fun []
  (pull conn '[:name {:to 2}] 1))

(defn add-entity [e]
  (transact! conn [e]))

(defn nodes []
  (let [r (q '[:find ?e ?name
               :where
               [?e :name ?name]]
             conn)]
    (reaction
      (vec (for [[id name] @r]
             {:db/id id
              :name name})))))

(defn edges []
  (let [r (q '[:find ?e ?from ?to
               :where
               [?e :from ?from]
               [?e :to ?to]]
             conn)]
    (reaction
      (vec (for [[id from to] @r]
             {:db/id id
              :from from
              :to to})))))
