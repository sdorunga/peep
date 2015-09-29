(defproject peep "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [compojure "1.3.1"]
                 [ring/ring-defaults "0.1.2"]
                 [ring/ring-json "0.4.0"]
                 [com.novemberain/monger "3.0.0-rc2"]
                 [hiccup "1.0.5"]
                 [com.cemerick/friend "0.2.0"]
                 [friend-oauth2 "0.1.1"]
                 [environ "1.0.1"]
                 [tentacles "0.3.0"]]
  :plugins [[lein-ring "0.8.13"]
            [lein-environ "1.0.1"]]
  :ring {:handler peep.handler/app}
  :profiles
  {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                        [ring-mock "0.1.5"]]}
   :uberjar {:aot :all}
   })
