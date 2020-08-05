(ns sample2.calc-page
  (:require
    [reagent.core :as r]
    [sample2.ajax :as ajax]
    [ajax.core :refer [GET POST]]
    [reitit.core :as reitit]
    [clojure.string :as string]))


(defonce equations (r/atom {:1 {:x 1 :y 0 :op "+" :total 1}
                            :2 {:x 3 :y 1 :op "-" :total 2}
                            :3 {:x 12 :y 4 :op "/" :total 3}
                            :4 {:x 2 :y 2 :op "*" :total 4}}))


(defn- set-key* [idx k new-val]
  (swap! equations assoc-in [idx k] new-val))

(defn do-math* [idx]
  (let [data (get @equations idx)
        x    (:x data)
        y    (:y data)
        op   (:op data)
        path (str "/api/math/"
                  (condp = op
                    "+" "plus"
                    "-" "minus"
                    "*" "multi"
                    "/" "div"))]
    (POST path
          {:headers {"Accept" "application/transit+json"}
           :params  {:x x :y y}
           :handler #(set-key* idx :total (:total %))})))


(defn input-field [id idx data]
  [:input.text
   {:type        :number
    :style       {:font-size :xx-large}
    :value       (id data)
    :placeholder (name id)
    :on-change   #(do (set-key* idx id (js/parseInt (-> % .-target .-value)))
                      (do-math* idx))}])


(defn colored [x]
  (cond
    (< x 19) "LightGreen"
    (<= x 49) "LightBlue"
    (>= x 50) "LightSalmon"))

(defn get-color [data]
  (let [v (:total data)]
    {:style {:font-weight "bold"
             :font-size :xx-large
             :color (colored v)}}))

(def style {:style {:font-weight "bold" :font-size :xx-large}})


(defn- make-row [idx data]
  [:div
   [input-field :x idx data]
   [:span style "  " (:op data) "  "]
   [input-field :y idx data]
   [:span style "  =  "]
   [:span (get-color data) (:total data)]])


(defn calc-page []
  [:div {:style {:width "100%" :height "100%"}}
   [:p.title "Hellooooo Sample 2!!"]
   [:div
    (make-row :1 (:1 @equations))
    (make-row :2 (:2 @equations))
    (make-row :3 (:3 @equations))
    (make-row :4 (:4 @equations))]])