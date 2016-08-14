(ns voterx.views.draw
  (:require
    [reagent.core :as reagent]
    [voterx.firebase :as firebase]
    [devcards.core]
    [clojure.string :as string])
  (:require-macros
    [devcards.core :refer [defcard-rg]]))

(defn xy [e]
  (let [rect (.getBoundingClientRect (.-currentTarget e))]
    [(- (.-clientX e) (.-left rect))
     (- (.-clientY e) (.-top rect))]))

(defn prepare [path]
  (update-in path [1 :d] #(string/join " " %)))

(defn draw [{:keys [save]}]
  (let [svg (reagent/atom [])
        pen-down? (reagent/atom false)
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
            (save @svg)))]
    (fn a-draw []
      [:svg
       {:style {:border "1px solid"
                :cursor "crosshair"
                ;; TODO: use css and all browsers
                :-webkit-user-select "none"}
        :width 400
        :height 400
        ;;:on-touch-start
        :on-mouse-over start-path
        :on-mouse-down start-path
        ;;:on-touch-move
        :on-mouse-move continue-path
        ;;:on-touch-end
        :on-mouse-up end-path
        ;;:on-touch-cancel
        :on-mouse-out end-path}
       (into
         ;; don't really need pointer-events none with currentTarget
         [:g {:style {:pointer-events "none"}
              :fill "none"
              :stroke "black"
              :stroke-width 5}]
         (for [elem @svg]
           (prepare elem)))])))

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
          (prepare elem))))))

(defcard-rg draw-card
  draw)
