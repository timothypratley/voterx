(ns voterx.views.login
  (:require [reagent.core :as reagent]))

(defonce user
  (reagent/atom nil))

(defn init []
  (.onAuthStateChanged
    (js/firebase.auth)
    (fn auth-state-changed [user-obj]
      ;; TODO: what is the type of user-obj?
      (reset! user {:photoURL (.-photoURL user-obj)
                    :displayName (.-displayName user-obj)}))
    (fn auth-error [error]
      (js/console.log error))))

(defn login-view []
  [:div
   {:style {:float "right"}}
   (if-let [{:keys [photoURL displayName]} @user]
     [:span
      [:button.mdl-button.mdl-js-button.mdl-button--fab.mdl-button--colored
       {:on-click
        (fn logout-click [e]
          ;; TODO: add then/error handlers
          (.signOut (js/firebase.auth))
          (reset! user nil))
        :title displayName
        :style {:background-image (str "url(" photoURL ")")
                :background-size "cover"
                :background-repeat "no-repeat"}}]]
     [:button.mdl-button.mdl-js-button.mdl-button--raised.mdl-button--colored
      {:on-click
       (fn login-click [e]
         (.signInWithPopup
           (js/firebase.auth.)
           (js/firebase.auth.GoogleAuthProvider.)))}
      "Login with Google"])])
