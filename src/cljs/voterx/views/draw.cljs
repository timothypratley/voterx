(ns voterx.views.draw
  (:require
    [reagent.core :as reagent]
    [voterx.firebase :as firebase]
    [devcards.core]
    [clojure.string :as string])
  (:require-macros
    [devcards.core :refer [defcard-rg]]))

(defn xy [e]
  (let [rect (.getBoundingClientRect (or (.-currentTarget e) (.-target e)))]
    [(- (.-clientX e) (.-left rect))
     (- (.-clientY e) (.-top rect))]))

(defn prepare [path mode]
  (if (= mode ::edit)
    (into
      [:g
       (update-in path [1 :d] #(string/join " " %))]
      (for [[x y] (partition 2 (filter number? (get-in path [1 :d])))]
        [:circle {:cx x :cy y :r 5 :stroke "blue" :stroke-width 1}]))
    (update-in path [1 :d] #(string/join " " %))))

(defn one-touch-handler [f]
  (fn a-one-touch-handler [e]
    (when (= (.. e -targetTouches -length) 1)
      (.preventDefault e)
      (let [touch (aget e "targetTouches" 0)]
        (f touch)))))

(defn draw [{:keys [save]}]
  (let [svg (reagent/atom [])
        img (reagent/atom nil)
        pen-down? (reagent/atom false)
        mode (reagent/atom ::draw)
        selected (reagent/atom nil)
        start-path
        (fn start-path [e]
          (when (not= (.-buttons e) 0)
            (reset! pen-down? true)
            (let [[x y] (xy e)]
              (swap! svg conj [:path {:d ['M x y 'L x y]}]))))
        continue-path
        (fn continue-path [e]
          (when @pen-down?
            (let [[x y] (xy e)]
              (swap! svg #(update-in % [(dec (count %)) 1 :d] conj x y)))))
        end-path
        (fn end-path [e]
          (continue-path e)
          (reset! pen-down? false)
          (when save
            (save @svg)))
        select
        (fn [e]
          (reset! selected (.-target e)))
        drag
        (fn [e]
          (prn "drag"))
        drop
        (fn [e]
          (prn "drop"))]
    (fn a-draw [attrs]
      [:div
       [:svg
        (merge-with
          merge
          {:style {:border "1px solid"
                   ;; TODO: use css and all browsers
                   :-webkit-user-select "none"}
           :width 400
           :height 400}
          (if (= @mode ::edit)
            {:style {:cursor "move"}
             :on-touch-start (one-touch-handler select)
             :on-mouse-down select
             :on-mouse-over select
             :on-touch-move (one-touch-handler drag)
             :on-mouse-move drag
             :on-touch-end (one-touch-handler drop)
             :on-mouse-up drop
             :on-touch-cancel (one-touch-handler drop)
             :on-mouse-out end-path}
            {:style {:cursor "crosshair"}
             :on-touch-start (one-touch-handler start-path)
             :on-mouse-down start-path
             :on-mouse-over start-path
             :on-touch-move (one-touch-handler continue-path)
             :on-mouse-move continue-path
             :on-touch-end (one-touch-handler end-path)
             :on-mouse-up end-path
             :on-touch-cancel (one-touch-handler end-path)
             :on-mouse-out end-path}))
        [:image {:xlink-href @img
                 :width "100%"
                 :height "100%"
                 :opacity 0.3}]
        (into
          ;; don't really need pointer-events none with currentTarget
          [:g {:style {:pointer-events "none"}
               :fill "none"
               :stroke "black"
               :stroke-width 5}]
          (for [elem @svg]
            (prepare elem @mode)))]
       [:div
        (if (= @mode ::edit)
          [:span.mdl-button.mdl-button--icon
           {:on-click
            (fn [e]
              (reset! mode ::draw))}
           [:i.material-icons "edit"]]
          [:span.mdl-button.mdl-button--icon
           {:on-click
            (fn [e]
              (reset! mode ::edit))}
           [:i.material-icons "adjust"]])
        [:span.mdl-button.mdl-button--icon
         {:on-click
          (fn [e]
            (save @svg))}
         [:i.material-icons "save"]]
        (if @img
          [:span.mdl-button.mdl-button--icon.active
           {:on-click
            (fn image-off [e]
              (reset! img nil))}
           [:i.material-icons "image"]]
          [:label
           [:input
            {:type "file"
             :accept "image/*"
             :style {:display "none"}
             :on-change
             (fn image-selected [e]
               (let [r (js/FileReader.)]
                 (set! (.-onload r)
                       (fn [e]
                         (reset! img (.. e -target -result))))
                 (.readAsDataURL r (aget (.. e -target -files) 0))))}]
           [:span.mdl-button.mdl-button--icon [:i.material-icons "image"]]])
        [:span.mdl-button.mdl-button--icon [:i.material-icons "undo"]]
        [:span.mdl-button.mdl-button--icon [:i.material-icons "redo"]]
        [:span.mdl-button.mdl-button--icon
         {:on-click
          (fn [e]
            (reset! img nil)
            (reset! svg []))}
         [:i.material-icons "delete"]]]])))

(defn view [svgs]
  (into
    [:svg
     {:style {:border "1px solid"
              :cursor "none"
              :-webkit-user-select "none"}
      :width 400
      :height 400}]
    (for [[properties svg] svgs]
      (into
        [:g (merge {:fill "none"
                    :stroke-width 5}
                   properties)]
        (for [elem svg]
          (prepare elem ::draw))))))

(defcard-rg draw-card
  draw)
