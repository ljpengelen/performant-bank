(ns bank.core
  (:require [bank.routes :refer [app]]
            [config.core :refer [env]]
            [hikari-cp.core :as hcp]
            [integrant.core :as ig]
            [migratus.core :as migratus]
            [org.httpkit.server :as http-kit])
  (:gen-class))

(defn system-config [db]
  {::datasource (-> env :db db)
   ::migrations {:datasource (ig/ref ::datasource)
                 :migration-dir (str "migrations/" (name db))}
   ::server {:datasource (ig/ref ::datasource)
             :migrations (ig/ref ::migrations)
             :port (:port env)}})

(defmethod ig/init-key ::datasource [_ {:keys [jdbc-url username password maximum-pool-size]}]
  (hcp/make-datasource {:jdbc-url jdbc-url
                        :username username
                        :password password
                        :maximum-pool-size maximum-pool-size}))

(defmethod ig/halt-key! ::datasource [_ datasource]
  (hcp/close-datasource datasource))

(defmethod ig/init-key ::migrations [_ {:keys [datasource migration-dir]}]
  (migratus/migrate {:store :database
                     :migration-dir migration-dir
                     :db {:datasource datasource}}))

(defmethod ig/init-key ::server [_ {:keys [datasource port]}]
  (http-kit/run-server (app datasource) {:port port :join? false}))

(defmethod ig/halt-key! ::server [_ server]
  (server))

(defn -main [& _]
  (ig/init (system-config :sqlite)))
