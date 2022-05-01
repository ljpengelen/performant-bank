(ns bank.db
  (:require [config.core :refer [env reload-env]]
            [criterium.core :as criterium]
            [hikari-cp.core :as hcp]
            [hugsql.adapter.next-jdbc :as next-adapter]
            [hugsql.core :as hugsql]))

(hugsql/set-adapter! (next-adapter/hugsql-adapter-next-jdbc))

(hugsql/def-db-fns "bank/db.sql")
(declare create-account!)

(defonce datasource (atom nil))

(defn create-datasource! [username password jdbc-url]
  (when @datasource
    (hcp/close-datasource @datasource)
    (reset! datasource nil))
  (reset! datasource (hcp/make-datasource {:user username
                                           :password password
                                           :jdbc-url jdbc-url})))

(comment
  (reload-env)
  (let [{:keys [username password jdbc-url]} (:db env)]
    (create-datasource! username password jdbc-url))
  @datasource
  (create-account! @datasource {:name "Luc Engelen"})
  (criterium/quick-bench (create-account! @datasource {:name "Luc Engelen"})))
