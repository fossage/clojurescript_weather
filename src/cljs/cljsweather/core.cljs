(ns cljsweather.core
  (:require
   [goog.date.Date]
   [reagent.core :as reagent :refer [atom]]
   [cljs-time.core :as time :refer [today-at]]
   [cljs-time.format :as format]
   [cljs-http.client :as http]
   [cljs.core.async :refer [<!]])
  (:require-macros [cljs.core.async.macros :refer [go]]))

;; -------------------------
;; Page components

; (defn items-page []
;   (fn []
;     [:span.main
;      [:h1 "The items of cljsweather"]
;      [:ul (map (fn [item-id]
;                  [:li {:name (str "item-" item-id) :key (str "item-" item-id)}
;                   [:a {:href (path-for :item {:item-id item-id})} "Item: " item-id]])
;                (range 1 60))]]))

(def weather-data (reagent/atom {:min nil :max nil :current nil}))
(def api-base-path "https://api.openweathermap.org/data/2.5/")

(defn assemble-request-url [request-type location]
  (str api-base-path request-type "?q=" location "&mode=json&appid=c36043dd046f0d256302ccfa30d8c6ad&units=imperial"))

(defn make-request [request-type location callback]
  (go (let [response (<! (http/get (assemble-request-url request-type location)  {:with-credentials? false}))] (callback (:body response)))))

(defn fetch-current-weather [city callback]
  (make-request "weather" city callback))


(defn current-weather []
  (let [get-formatted-date #(format/unparse (format/formatter "MMMM D, h:mm a") (goog.date.DateTime.))
        state (reagent/atom {:city-text "" :date (get-formatted-date)})]

    (.setInterval js/window (fn [] (swap! state assoc :date (get-formatted-date))), 1000)

    (fn [current-weather-data city on-change-weather]
      (let [handle-click #(let [trimmed-text (-> (:city-text @state) str clojure.string/trim)]
                            (on-change-weather trimmed-text)
                            (swap! state assoc :city-text ""))]

        [:header.current-weather-container
         [:div
          [:p.current-time-text (:date @state)]

          (if (nil? current-weather-data)
            nil
            [:p.min-max-text "Max º | Min º"])]

         [:div.city-container
          [:h1 city]

          [:input {:type "text"
                   :value (:city-text @state)
                   :placeholder "Enter city"
                   :on-change #(swap! state assoc :city-text (-> % .-target .-value))}]

          [:button {:on-click handle-click} "Submit"]]]))))
;; -------------------------
;; Page mounting component
(defn app []
  (let [state (reagent/atom {:current-city "Seattle" :forecast nil :current-weather nil})
        handle-city-changed #(fetch-current-weather % (fn [body] (do ((swap! state assoc :current-weather body)
                                                                      (swap! state assoc :current-city %)))))]

    (fn []
      [:div.App
       [current-weather nil (:current-city @state) handle-city-changed]])))

;; -------------------------
;; Initialize app

(defn mount-root []
  (reagent/render [app] (.getElementById js/document "app")))

(defn init! []
  (mount-root))
