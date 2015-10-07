(ns peep.routes.repo-management
  (:require [compojure.core :refer [GET POST defroutes]]
            [ring.util.response :as response]
            [hiccup.page :refer :all]
            [hiccup.element :refer :all]
            [cemerick.friend        :as friend]
            [friend-oauth2.workflow :as oauth2]
            [friend-oauth2.util     :refer [format-config-uri get-access-token-from-params]]
            [tentacles.users :as users]
            [tentacles.repos :as repos]
            [environ.core :refer [env]]))

(def client-config
  {:client-id     (env :client-id)
   :client-secret (env :client-secret)
   :callback      {:domain (env :domain) :path "/github.callback"}})

(def uri-config
  {:authentication-uri {:url (env :authentication-url)
                        :query {:client_id (:client-id client-config)
                                :response_type "code"
                                :redirect_uri (format-config-uri client-config)
                                :scope "repo"
                                }}

   :access-token-uri {:url (env :access-token-url)
                      :query {:client_id (:client-id client-config)
                              :client_secret (:client-secret client-config)
                              :grant_type "authorization_code"
                              :redirect_uri (format-config-uri client-config)
                              }}})

(defn credential-fn
    [token]
    {:identity token
     :roles #{::user}})

(def friend-config
  {:allow-anon? true
   :workflows   [(oauth2/workflow
                  {:client-config client-config
                   :uri-config uri-config
                   :access-token-parsefn get-access-token-from-params
                   :credential-fn credential-fn})
                 ]})

(defn auth
  [token]
  {:oauth-token token :client-id (:client-id uri-config) :client-token (:client-token uri-config)})
(defn get-token
  [request]
  (:access-token (:current (friend/identity request))))
(defn repositories
  [token]
  (repos/repos (merge (auth token) {:affiliation "owner"})))
(defn hooks
  [user repo token]
  (repos/hooks (:login user) repo (auth token)))

(defn repositories-with-hooks
  [user token]
  (let [repos (map :name (repositories token))]
    (->> (for [repo repos] [repo (future (hooks user repo token))])
         (map (fn [[repo hooks]] [repo (deref hooks)]))
         (into {}))))

(defn toggle-repo-link
  [[name webhooks]]
  (if-let [hook (first (filter #(re-find (re-pattern (env :domain)) (get-in % [:config :url] "")) webhooks))]
    (link-to (str "remove-webhook/" name "/" (:id hook)) (str "Remove " name))
    (link-to (str "toggle-repo/" name) (str "Add " name))))

(defn render-repos
  [user token]
  (unordered-list (for [m (repositories-with-hooks user token)
                        ] [:div (toggle-repo-link m) (unordered-list (map #(get-in % [:config :url]) (last m)))])))

(defn add-hook
  [user repo-name req]
  (let [token (:access-token (:current (friend/identity req)))]
    (repos/create-hook (:login user) repo-name "web" {:url (str (env :domain) "/webhooks") :content_type "json"} (merge (auth token) {:active true :events ["*" "push" "pull_request"] }))))

(defn remove-hook
  [user repo-name hook-id req]
  (let [token (:access-token (:current (friend/identity req)))]
    (repos/delete-hook (:login user) repo-name hook-id (auth token))))

(defroutes routes
  (GET "/" req
       (let [token (get-token req)
             repos (repositories token)
             user (users/me (auth token))
             repos-with-hooks (repositories-with-hooks user token)]
         (html5 [:head
                 [:title "Peep Time"]]
                [:body
                 [:div "Repo activity tracking"]
                 [:a {:href "github.callback"} "Login with GitHub"]
                 (when token
                   [:section {:class "repositories"}
                    [:div (str "Token: " token)]
                    [:div (str "Welcome " (:login user))]
                    [:div "Repos: "
                     (render-repos user token)]])])))
  (GET "/toggle-repo/:name" {:keys [params] :as req}
       (let [token (get-token req)
             user (users/me (auth token))]
         (add-hook user (:name params) req))
       (response/redirect "/"))
  (GET "/remove-webhook/:name/:id" {:keys [params] :as req}
       (let [token (get-token req)
             user (users/me (auth token))]
         (remove-hook user (:name params) (:id params) req))
       (response/redirect "/")))
