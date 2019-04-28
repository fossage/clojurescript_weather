(ns cljsweather.core
  (:require
   [goog.date.Date]
   [clojure.string]
   [reagent.core :as reagent :refer [atom]]
   [cljs-time.core :as time :refer [today-at date-time]]
   [cljs-time.format :as format]
   [cljs-time.coerce :as coerce]
   [cljs-http.client :as http]
   [cljs.core.async :refer [<!]])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(def api-base-path "https://api.openweathermap.org/data/2.5/")

(defn assemble-request-url [request-type location]
  (str
   api-base-path
   request-type
   "?q="
   location
   "&mode=json&appid=c36043dd046f0d256302ccfa30d8c6ad&units=imperial"))

(defn make-request [request-type location callback]
  (go (let
       [response
        (<! (http/get
             (assemble-request-url request-type location)
             {:with-credentials? false}))]
        (callback (:body response)))))

(defn fetch-current-weather [city callback]
  (make-request "weather" city callback))

(defn fetch-forecast [city callback]
  (make-request "forecast/daily" city callback))

(defn forecast [forecast-data]
  (if (nil? forecast-data)
    nil
    [:div.container
     (map (fn [forecast-item]
            (let [temp-text #(-> (get forecast-item %) (Math.floor) (str "º"))]
              [:div.weather-item-container {:key (get forecast-item :dt)}
               [:div.weather-item-inner-container
                [:div
                 [:h3.date-heading (format/unparse (format/formatter "EEEE, MMM d") (coerce/from-long (* (get forecast-item :dt) 1000)))]
                 [:p.weather-description (-> (get forecast-item :description) (clojure.string/capitalize))]]]
               [:div.temp-and-img-container
                [:img.weather-img {:alt "" :src ""}]
                [:div.temp-min-max-container
                 [:p.temp-min-max-text (temp-text :max)]
                 [:p.temp-min-max-text (temp-text :min)]]]])) forecast-data)]))


(defn current-weather []
  (let [get-formatted-date #(format/unparse (format/formatter "MMMM D, h:mm a") (time/date-time nil))
        state (reagent/atom {:city-text "" :date (get-formatted-date)})]

    (.setInterval js/window (fn [] (swap! state assoc :date (get-formatted-date))), 10000)

    (fn [current-weather-data city on-change-weather]
      (let [handle-click #(let [trimmed-text (-> (:city-text @state) str clojure.string/trim)]
                            (on-change-weather trimmed-text)
                            (swap! state assoc :city-text ""))]

        [:header.current-weather-container
         [:div
          [:p.current-time-text (:date @state)]

          (if (nil? current-weather-data)
            nil
            [:p.min-max-text
             (str
              '"Max "
              (get current-weather-data :max)
              "º | Min "
              (get current-weather-data :min) "º")])
          (if (nil? current-weather-data)
            nil
            [:h1.current-temp-text (str (get current-weather-data :current) "ºF")])]

         [:div.city-container
          [:h1 (clojure.string/capitalize city)]

          [:input
           {:type "text"
            :value (:city-text @state)
            :placeholder "Enter city"
            :on-change #(swap! state assoc :city-text (-> % .-target .-value))}]

          [:button {:on-click handle-click} "Submit"]]]))))
;; -------------------------
;; Page mounting component
(defn app []
  (let [state (reagent/atom {:current-city "Seattle"
                             :forecast nil
                             :current-weather nil})

        handle-current-weather-response #(fn [body]
                                           (do
                                             ((swap!
                                               state assoc :current-weather
                                               {:current (-> (get-in body [:main :temp]) (Math.floor))
                                                :min (-> (get-in body [:main :temp_min]) (Math.floor))
                                                :max (-> (get-in body [:main :temp_max]) (Math.floor))})
                                              (swap! state assoc :current-city %))))
        handle-forecast-response #(do
                                    ((swap!
                                      state assoc :forecast
                                      (map (fn [day] {:dt (get day :dt)
                                                      :description (get-in day [:weather 0 :description])
                                                      :icon (get-in day [:weather 0 :icon])
                                                      :max (get-in day [:temp :max])
                                                      :min (get-in day [:temp :min])}) (get % :list)))))

        handle-city-changed #(do (fetch-current-weather % (handle-current-weather-response %))
                                 (fetch-forecast % handle-forecast-response))]

    (.setInterval js/window (fetch-current-weather
                             (:current-city @state)
                             (handle-current-weather-response (:current-city @state))), 500000)
    (fetch-forecast (:current-city @state) handle-forecast-response)

    (fn []
      [:div.App
       [current-weather (:current-weather @state) (:current-city @state) handle-city-changed]
       [forecast (:forecast @state)]])))

;; -------------------------
;; Initialize app

(defn mount-root []
  (reagent/render [app] (.getElementById js/document "app")))

(defn init! []
  (mount-root))
