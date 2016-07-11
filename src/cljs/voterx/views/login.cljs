(ns voterx.views.login
  (:require
    [voterx.firebase :as firebase]))

(defn login-view []
  [:div
   {:style {:float "right"}}
   (if-let [{:keys [photoURL displayName]} @firebase/user]
     [:span
      [:button.mdl-button.mdl-js-button.mdl-button--fab.mdl-button--colored
       {:on-click
        (fn logout-click [e]
          (firebase/logout))
        :title displayName
        :style {:background-image (str "url(" photoURL ")")
                :background-size "cover"
                :background-repeat "no-repeat"}}]]
     [:button.mdl-button.mdl-js-button.mdl-button--raised.mdl-button--colored
      {:on-click
       (fn login-click [e]
         (firebase/sign-in-with-popup))}
      "Login with Google"])])
