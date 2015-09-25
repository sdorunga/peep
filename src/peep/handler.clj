(ns peep.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults site-defaults]]
            [ring.middleware.json :refer [wrap-json-body]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.session.cookie :refer [cookie-store]]
            [ring.middleware.session :refer [wrap-session]]
            [ring.util.response :refer [response]]
            [cemerick.friend        :as friend]
            [peep.event :refer [store-event]]
            [peep.routes.repo-management :as repo-management]))

(defroutes app-routes
  repo-management/routes
  (POST "/webhooks" {:keys [headers body]} (println (get headers "x-github-event") headers) (store-event (get headers "x-github-event") body){:status 200})
  (route/not-found "Not Found"))

(def app
  (-> app-routes
      (friend/authenticate repo-management/friend-config)
      (#(fn [request] (let [response (% request)] (println request) response)))
      (wrap-params)
      (wrap-session {:store (cookie-store {:key "awefawefawefawef"})})
      (wrap-defaults (-> site-defaults
                         (assoc-in [:security :anti-forgery] false)))
      ;;(logger/wrap-with-logger)
      ;;(wrap-json-body {:keywords? true :bigdecimals? true})
      ))
