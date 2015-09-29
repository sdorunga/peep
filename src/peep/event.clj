(ns peep.event
  (:require [monger.core :as mg]
            [monger.operators :refer :all]
            [monger.joda-time]
            [monger.collection :as mc]
            [environ.core :refer [env]])
  (:import org.bson.types.ObjectId))

(def ^:private db
  (:db (mg/connect-via-uri (str (env :mongo-uri) "/" (env :mongo-db)))))

(defn store-event
  [type event]
  (mc/insert db "github-events" (merge { :_id (ObjectId.) :event-type type :created_at (java.util.Date.)} event)))
