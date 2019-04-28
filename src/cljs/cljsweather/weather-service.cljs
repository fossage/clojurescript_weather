(ns cljsweather.weather-service
  (:require  [cljs-http.client :as http]
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