(ns peep.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [ring.middleware.json :refer [wrap-json-body]]
            [ring.util.response :refer [response]]
            [peep.event :refer [store-event]]))

(defroutes app-routes
  (GET "/" [request] (println request) "Hello World")
  (POST "/webhooks" {:keys [headers body]} (println (get headers "x-github-event") headers) (store-event (get headers "x-github-event") body){:status 200})
  (route/not-found "Not Found"))

(def app
  (-> app-routes
      (wrap-defaults api-defaults)
      (wrap-json-body {:keywords? true :bigdecimals? true})))
