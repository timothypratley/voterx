(ns voterx.db
  (:require
    [voterx.combinatorics :as combinatorics]
    [cljs.tools.reader.edn :as edn]
    [reagent.core :as reagent]
    [posh.reagent :refer [pull q posh! transact!]]
    [datascript.core :as d])
  (:require-macros
    [reagent.ratom :refer [reaction]]))

(def schema
  {:to {:db/cardinality :db.cardinality/many}})

(defn init [conns uid]
  (let [conn (d/create-conn schema)]
    (posh! conn)
    (swap! conns assoc uid conn)
    conn))

(defn add-conn [conns uid db]
  (swap! conns assoc uid
         (doto (d/create-conn schema)
           (d/reset-conn! (edn/read-string {:readers d/data-readers} db))
           (posh!))))

(defn add-entity [conn e]
  (transact! conn [e]))

(defn retract [conn id]
  (transact! conn [:db.fn/retractEntity id]))

(defn nodes-q [conn]
  (q '[:find ?e ?name
       :where
       [?e :name ?name]]
     conn))

(defn nodes [conns]
  (reaction
    (doall
      (for [[uid conn] @conns
            [id name] @(nodes-q conn)]
        {:db/id (str uid "-" id)
         :uid uid
         :name name}))))

(defn edges-q [conn]
  (q '[:find ?e ?from ?to
       :where
       [?e :from ?from]
       [?e :to ?to]]
     conn))

(defn edges [conns]
  (reaction
    (doall
      (concat
        (for [[uid conn] @conns
              [id from to] @(edges-q conn)]
          {:db/id (str uid "-" id)
           :uid uid
           :from (str uid "-" from)
           :to (str uid "-" to)})
        (for [[name nodes] (group-by :name @(nodes conns))
              [a b] (combinatorics/combinations nodes 2)]
          {:db/id (str (:db/id a) "-" (:db/id b))
           :from (:db/id a)
           :to (:db/id b)})))))
