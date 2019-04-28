(ns cljsweather.core
  (:require
   [reagent.core :as reagent :refer [atom]]
   [cljsweather.weather-service :as ws]
   [cljsweather.components.forecast :as forecast]
   [cljsweather.current-weather :as current-weather]))

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

        handle-city-changed #(do (ws/fetch-current-weather % (handle-current-weather-response %))
                                 (ws/fetch-forecast % handle-forecast-response))]

    (.setInterval js/window (ws/fetch-current-weather
                             (:current-city @state)
                             (handle-current-weather-response (:current-city @state))), 500000)
    (ws/fetch-forecast (:current-city @state) handle-forecast-response)

    (fn []
      [:div.App
       [current-weather/component (:current-weather @state) (:current-city @state) handle-city-changed]
       [forecast/component (:forecast @state)]])))

;; -------------------------
;; Initialize app
(defn mount-root []
  (reagent/render [app] (.getElementById js/document "app")))

(defn init! []
  (mount-root))
