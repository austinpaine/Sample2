(ns sample2.core
  (:require
    [reagent.core :as r]
    [reagent.dom :as rdom]
    [goog.events :as events]
    [goog.history.EventType :as HistoryEventType]
    [markdown.core :refer [md->html]]
    [sample2.ajax :as ajax]
    [ajax.core :refer [GET POST]]
    [reitit.core :as reitit]
    [sample2.calc-page :refer [calc-page]]
    [clojure.string :as string])
  (:import goog.History))

(defonce session (r/atom {:page :home}))

(defn nav-link [uri title page]
  [:a.navbar-item
   {:href   uri
    :class (when (= page (:page @session)) "is-active")}
   title])

(defn navbar []
  (r/with-let [expanded? (r/atom false)]
              [:nav.navbar.is-info>div.container
               [:div.navbar-brand
                [:a.navbar-item {:href "/" :style {:font-weight :bold}} "sample2"]
                [:span.navbar-burger.burger
                 {:data-target :nav-menu
                  :on-click #(swap! expanded? not)
                  :class (when @expanded? :is-active)}
                 [:span][:span][:span]]]
               [:div#nav-menu.navbar-menu
                {:class (when @expanded? :is-active)}
                [:div.navbar-start
                 [nav-link "#/" "Home" :home]
                 [nav-link "#/about" "About" :about]
                 [nav-link "#/calc" "Calculator" :calculator]]]]))

(defn about-page []
  [:section.section>div.container>div.content
   [:img {:src "/img/warning_clojure.png"}]])

(defonce new-eq {:x 0 :y 0 :op "+" :total 0})

(defonce equations (r/atom [{:x 0 :y 0 :op "+" :total 0}
                            {:x 3 :y 1 :op "+" :total 4}]))

(defn- new-equation []
  (swap! equations conj new-eq))

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
    :on-change   #(do(prn "clicked " id idx)
                     (set-key* idx id (js/parseInt (-> % .-target .-value)))
                     (do-math* idx))}])

(defn colored-field [result]
  [:input.text {:class (cond
                         (< result 0) "negative-result"
                         (and (<= 0 result) (< result 20)) "small-result"
                         (and (<= 20 result) (< result 50)) "medium-result"
                         (<= 50 result) "large-result")
                :style {:font-weight "bold"
                        :font-size :xx-large}
                :readOnly true
                :value (str result)}])


(defn- make-row [idx data]
  [:div
   [input-field :x idx data]
   [:select {:style     {:font-size :xx-large}
             :on-change #(do
                           (set-key* idx :op (-> % .-target .-value))
                           (do-math* idx))}
    (map #(into ^{:key %} [:option %]) ["+" "-" "*" "/"])]
   [input-field :y idx data]
   [:span "="]
   (colored-field (:total data))])

(defn home-page []
  [:div {:style {:width "100%" :height "100%"}}
   [:p.title "Hellooooo Sample 2!!"]
   [:div.button.is-medium.is-primary {:on-click #(new-equation)} "Add Equation"]
   (doall
     (for [idx (range (count @equations))]
       (let [data (get @equations idx)]
         (make-row idx data))))])


;(defn home-page []
;  [:div {:style {:width "100%" :height "100%"}}
;   [:p.title "Hellooooo Sample 2!!"]
;   [:div
;    [:input.text {:placeholder (:x @equation)
;                  :on-change #(swap! equation assoc :x (js/parseInt (-> % .-target .-value)))}]
;
;    [:span "  +  "]
;
;    [:input.text {:placeholder (:y @equation)
;                  :on-change #(swap! equation assoc :y (js/parseInt (-> % .-target .-value)))}]
;
;    [:button {:style {:margin "5px"}
;              :on-click #(do-math*)} "   =   " ]
;
;    [:input.text {:readOnly true
;                  :value (:total @equation)}]]])

(def pages
  {:home #'home-page
   :about #'about-page
   :calculator #'calc-page})

(defn page []
  [(pages (:page @session))])

;; -------------------------
;; Routes

(def router
  (reitit/router
    [["/" :home]
     ["/about" :about]
     ["/calc" :calculator]]))

(defn match-route [uri]
  (->> (or (not-empty (string/replace uri #"^.*#" "")) "/")
       (reitit/match-by-path router)
       :data
       :name))
;; -------------------------
;; History
;; must be called after routes have been defined
(defn hook-browser-navigation! []
  (doto (History.)
    (events/listen
      HistoryEventType/NAVIGATE
      (fn [event]
        (swap! session assoc :page (match-route (.-token ^js event)))))
    (.setEnabled true)))

;; -------------------------
;; Initialize app
(defn fetch-docs! []
  (GET "/docs" {:handler #(swap! session assoc :docs %)}))

(defn mount-components []
  (rdom/render [#'navbar] (.getElementById js/document "navbar"))
  (rdom/render [#'page] (.getElementById js/document "app")))

(defn init! []
  (ajax/load-interceptors!)
  (fetch-docs!)
  (hook-browser-navigation!)
  (mount-components))
