(ns cljsweather.current-weather
  (:require
   [clojure.string]
   [reagent.core :as reagent :refer [atom]]
   [cljs-time.core :as time :refer [date-time]]
   [cljs-time.format :as format]))

(defn component []
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