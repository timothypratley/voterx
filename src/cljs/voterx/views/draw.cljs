(ns voterx.views.draw
  (:require
    [reagent.core :as reagent]
    [voterx.names :as names]
    [devcards.core]
    [clojure.string :as string]
    [voterx.firebase :as firebase]
    [cljs.tools.reader.edn :as edn])
  (:require-macros
    [devcards.core :refer [defcard-rg]]))

(defn xy [e [width height]]
  (let [rect (.getBoundingClientRect (or (.-currentTarget e) (.-target e)))]
    [(-> (- (.-clientX e) (.-left rect))
         (/ (.-width rect))
         (* width)
         (js/Math.round))
     (-> (- (.-clientY e) (.-top rect))
         (/ (.-height rect))
         (* height)
         (js/Math.round))]))

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

(defn draw [{:keys [svg save dims]}]
  (let [svg (or svg (reagent/atom []))
        title (reagent/atom (names/sketch-name))
        notes (reagent/atom nil)
        img (reagent/atom nil)
        pen-down? (reagent/atom false)
        mode (reagent/atom ::draw)
        selected (reagent/atom nil)
        start-path
        (fn start-path [e]
          (when (not= (.-buttons e) 0)
            (reset! pen-down? true)
            (let [[x y] (xy e dims)]
              (swap! svg conj [:path {:d ['M x y 'L x y]}]))))
        continue-path
        (fn continue-path [e]
          (when @pen-down?
            (let [[x y] (xy e dims)]
              (swap! svg #(update-in % [(dec (count %)) 1 :d] conj x y)))))
        end-path
        (fn end-path [e]
          (when @pen-down?
            (continue-path e)
            (reset! pen-down? false)
            (when save
              (save {:title @title
                     :svg @svg
                     :notes @notes}))))
        select
        (fn [e]
          (reset! selected (.-target e)))
        drag
        (fn [e]
          (prn "drag"))
        drop
        (fn [e]
          (prn "drop"))]
    (fn a-draw [{:keys [save dims]}]
      [:div
       [:svg
        (merge-with
          merge
          {:view-box (string/join " " (concat [0 0] dims))
           :style {:border "1px solid black"
                   ;; TODO: use css and all browsers
                   :-webkit-user-select "none"}}
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
        (when @img
          [:image {:xlink-href @img
                   :width "100%"
                   :height "100%"
                   :opacity 0.3}])
        (into
          [:g {:fill "none"
               :stroke "black"
               :stroke-width 5}]
          (for [elem @svg]
            (prepare elem @mode)))]
       [:input {:type "text"
                :style {:width "100%"}
                :default-value @title
                :on-change
                (fn title-changed [e]
                  (reset! title (.. e -target -value)))}]
       [:div
        [:span.mdl-button.mdl-button--icon
         {:on-click
          (fn click-save [e]
            (when save
              (save {:title @title
                     :svg @svg
                     :notes @notes})))}
         [:i.material-icons "save"]]
        (if (= @mode ::edit)
          [:span.mdl-button.mdl-button--icon
           {:on-click
            (fn draw-mode [e]
              (reset! mode ::draw))}
           [:i.material-icons "edit"]]
          [:span.mdl-button.mdl-button--icon
           {:on-click
            (fn edit-mode [e]
              (reset! mode ::edit))}
           [:i.material-icons "adjust"]])
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
           [:span.mdl-button.mdl-button--icon
            [:i.material-icons "image"]]])
        [:span.mdl-button.mdl-button--icon
         [:i.material-icons "undo"]]
        [:span.mdl-button.mdl-button--icon
         [:i.material-icons "redo"]]
        [:span.mdl-button.mdl-button--icon
         {:on-click
          (fn clear [e]
            (reset! img nil)
            (reset! svg []))}
         [:i.material-icons "delete"]]]
       [:textarea
        {:rows 5
         :style {:width "100%"}
         :on-change
         (fn notes-entered [e]
           (reset! notes (.. e -target -value)))}]])))

(defcard-rg draw-card
  [draw {:dims [400 400]}])

(defn prepare-svg [tag properties elems]
  (into
    [tag (merge {:fill "none"
                 :stroke "black"
                 :stroke-width 5}
               properties)]
    (for [elem elems]
      (prepare elem ::draw))))

(defn view-drawing [{:keys [uid id]}]
  [firebase/on ["users" uid "drawings" id]
   (fn [svg]
     [prepare-svg :svg
      {:view-box "0 0 400 400"
       :style {:border "1px solid black"
               :cursor "none"
               ;; TODO: moar browzazs
               :-webkit-user-select "none"}}
      (edn/read-string (some-> @svg (.-svg)))])])

(defn sorted-by-width []
  (let [ss (reagent/atom {"the" nil
                          "quick" nil
                          "brown" nil
                          "fox" nil})]
    (fn a-sorted-by-width []
      [:ul
       (for [[s width] (sort-by val @ss)]
         ^{:key s}
         [:li
          [:span
           {:ref (fn text-ref [elem]
                   (when elem
                     (swap! ss assoc s (.-width (.getBoundingClientRect elem)))))
            :visibility (if width "visible" "hidden")}
           s]])])))

(defcard-rg by-size
  sorted-by-width)
