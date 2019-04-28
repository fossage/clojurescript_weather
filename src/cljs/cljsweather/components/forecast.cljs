(ns cljsweather.components.forecast
  (:require
   [clojure.string]
   [cljs-time.format :as format]
   [cljs-time.coerce :as coerce]))

(defn component [forecast-data]
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