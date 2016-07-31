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

(defn db-ref [path]
  (.ref (js/firebase.database) (string/join "/" path)))

(defn save [path value]
  (.set (db-ref path) value))

(defonce user
  (reagent/atom nil))

(defn on-auth []
  (.onAuthStateChanged
    (js/firebase.auth)
    (fn auth-state-changed [user-obj]
      ;; TODO: better way of cljsizing user-obj?
      (let [uid (.-uid user-obj)
            display-name (.-displayName user-obj)
            photo-url (.-photoURL user-obj)]
        (save ["users" uid "settings"]
              #js {:photo-url photo-url
                   :display-name display-name})
        (reset! user {:photoURL photo-url
                      :displayName display-name
                      :uid uid})))
    (fn auth-error [error]
      (js/console.log error))))

(defn init []
  (js/firebase.initializeApp
    #js {:apiKey "AIzaSyDosF04KpvPslT5g0mzjZ0Q-paRluRWC-M"
         :authDomain "voterx-e88a1.firebaseapp.com"
         :databaseURL "https://voterx-e88a1.firebaseio.com"
         :storageBucket "voterx-e88a1.appspot.com"})
  (on-auth))

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

(defn once
  "Retreives the firebase state at path as a ratom that will be set when the state arrives."
  [path]
  (let [a (reagent/atom nil)]
    (.once
      (db-ref path)
      "value"
      (fn received-db [snapshot]
        (reset! a (.val snapshot))))
    a))

(defcard-rg once-card
  "This card will not update on changes, only when you refresh the page."
  (fn []
    (let [a (once ["test"])]
      (fn []
        [:div @a]))))

(defn on
  "Takes a path and a component.
  Component takes a ratom as it's first argument, and optional other arguments.
  The atom derefs to the firebase state at path."
  [path component]
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
       (fn render-listener [path component & args]
         (into [component a] args))})))

(defcard-rg on-card
  "Watch the console for messages. While hidden, no changes are listened to."
  (let [show? (reagent/atom true)]
    (fn []
      [:div
       [:button
        {:on-click
         (fn [e]
           (swap! show? not))}
        (if @show?
          "hide"
          "show")]
       (when @show?
         [on ["test"]
          (fn [a]
            (println "Received change" @a)
            [:div @a])])])))

(defn sign-in-with-popup []
  (.signInWithPopup
    (js/firebase.auth.)
    (js/firebase.auth.GoogleAuthProvider.)))

(defn logout []
  ;; TODO: add then/error handlers
  (.signOut (js/firebase.auth))
  (reset! user nil))

#_(defn save-db [uid]
  (.set
    (db-ref ["users" uid "db"])
    (pr-str @(or (@db/conns uid)
                 (db/init uid)))))

(defn dissoc-in
  "Dissociates an entry from a nested associative structure returning a new
  nested structure. keys is a sequence of keys. Any empty maps that result
  will not be present in the new structure."
  [m [k & ks :as keys]]
  (if ks
    (if-let [nextmap (get m k)]
      (let [newmap (dissoc-in nextmap ks)]
        (if (seq newmap)
          (assoc m k newmap)
          (dissoc m k)))
      m)
    (dissoc m k)))

(defn with-refs
  "Takes a component that will render with an atom, on, off and args.
  on and off take paths to listen/unlisten to.
  The atom derefs to the firebase state for listened to paths."
  [component & args]
  (let [refs (reagent/atom {})
        a (reagent/atom {})
        on (fn on [path]
             (when-not (@refs path)
               (swap! refs assoc path
                      (doto (db-ref path)
                        (.on "value" (fn [x]
                                       (swap! a assoc-in path (.val x))))))))
        off (fn off [path]
              (when-let [ref (@refs path)]
                (.off ref))
              (swap! refs dissoc path)
              (swap! a dissoc-in path))]
    (reagent/create-class
      {:display-name "listener"
       :component-will-unmount
       (fn will-unmount-listener [this]
         (doseq [ref (vals @refs)]
           (.off ref)))
       :reagent-render
       (fn render-listener [component & args]
         (into [component a on off] args))})))

(defcard-rg with-refs-card
  "Takes a component that will render with an atom, on, off and args.
  on and off take paths to listen/unlisten to.
  The atom derefs to the firebase state for listened to paths."
  (let [show? (reagent/atom true)]
    (fn []
      [:div
       [:button {:on-click (fn [e] (swap! show? not))} (if @show? "hide" "show")]
       (when @show?
         [with-refs
          (fn listening-component [a on off]
            (into
              [:div
               [:div (pr-str @a)]]
              (mapcat
                (fn [path]
                  [[:button {:on-click (fn [e] (on path))} (str "on " (pr-str path))]
                   [:button {:on-click (fn [e] (off path))} (str "off " (pr-str path))]])
                [["test"] ["test2"]])))])])))

(defn with-refs-only
  [add clear component & args]
  (let [refs (reagent/atom {})
        on (fn on [path]
             (when-not (@refs path)
               (swap! refs assoc path
                      (doto (db-ref path)
                        (.on "value" (fn [x]
                                       (add path (.val x))))))))
        off (fn off [path]
              (when-let [ref (@refs path)]
                (.off ref))
              (swap! refs dissoc path)
              (clear path))]
    (reagent/create-class
      {:display-name "listener"
       :component-will-unmount
       (fn will-unmount-listener [this]
         (doseq [ref (vals @refs)]
           (.off ref)))
       :reagent-render
       (fn render-listener [add clear component & args]
         (into [component on off] args))})))
