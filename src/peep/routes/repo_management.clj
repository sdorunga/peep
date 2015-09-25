(ns peep.routes.repo-management
  (:require [compojure.core :refer [GET POST defroutes]]
            [hiccup.page :refer :all]
            [hiccup.element :refer :all]
            [cemerick.friend        :as friend]
            [friend-oauth2.workflow :as oauth2]
            [friend-oauth2.util     :refer [format-config-uri get-access-token-from-params]]
            [tentacles.users :as users]
            [tentacles.repos :as repos]))

(def client-config
  {:client-id     "4298ab491bba7f6f1e0d" ;;(env :friend-oauth2-client-id)
   :client-secret "ee54929a579021658082ee76826897a59af2226f" ;;(env :friend-oauth2-client-secret)
   :callback      {:domain "http://1590298f.ngrok.com" :path "/github.callback"}})

(def uri-config
  {:authentication-uri {:url "https://github.com/login/oauth/authorize"
                        :query {:client_id (:client-id client-config)
                                :response_type "code"
                                :redirect_uri (format-config-uri client-config)
                                :scope "repo"
                                }}

   :access-token-uri {:url "https://github.com/login/oauth/access_token"
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


(defn toggle-hook
  [user repo-name]
  (println  (:login user) "sdaf asdf " repo-name)
  (repos/create-hook (:login user) repo-name "makers-hook" {:active true :events "*"} {:url "http://example.com"}))

(defroutes routes
  (GET "/" req
       (let [token (:access-token (:current (friend/identity req)))
             repos (repos/repos {:affiliation "owner" :oauth-token token :client-id "4298ab491bba7f6f1e0d" :client-token "ee54929a579021658082ee76826897a59af2226f"})
             user (users/me {:oauth-token token :client-id "4298ab491bba7f6f1e0d" :client-token "ee54929a579021658082ee76826897a59af2226f"})]
         (html5 [:head
                 [:title "Peep Time"]]
                [:body
                 [:div "Hello World"]
                 [:a {:href "github.callback"} "Click here to begin"]
                 [:div ]
                 [:div (str "Token: " token)]
                 (when token
                   [:div (str "Welcome " (:name user))]
                   [:div "Repos: "
                    (unordered-list (map #(link-to (str "toggle-repo/" %) %) (map :name repos)))])])))
  (GET "/toggle-repo/:name" {:keys [params] :as req}
       (let [token (:access-token (:current (friend/identity req)))
             user (users/me {:oauth-token token :client-id "4298ab491bba7f6f1e0d" :client-token "ee54929a579021658082ee76826897a59af2226f"})] (toggle-hook user (:name params))) "Success"))
