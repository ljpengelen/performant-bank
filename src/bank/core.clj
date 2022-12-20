(ns bank.core
  (:require [bank.db :as db]
            [bank.routes :refer [app]]
            [config.core :refer [env]]
            [hikari-cp.core :as hcp]
            [integrant.core :as ig]
            [migratus.core :as migratus]
            [org.httpkit.server :as http-kit])
  (:gen-class))

(def system-config
  {::datasource (get-in env [:db-config (:db env)])
   ::db-fns nil
   ::handler {:async? (:async? env)
              :datasource (ig/ref ::datasource)}
   ::migrations {:datasource (ig/ref ::datasource)
                 :migration-dir (get-in env [:db-config (:db env) :migration-dir])}
   ::server {:db-fns (ig/ref ::db-fns)
             :handler (ig/ref ::handler)
             :migrations (ig/ref ::migrations)
             :port (:port env)}})

(defmethod ig/init-key ::datasource [_ {:keys [jdbc-url username password maximum-pool-size]}]
  (hcp/make-datasource {:jdbc-url jdbc-url
                        :username username
                        :password password
                        :maximum-pool-size maximum-pool-size}))

(defmethod ig/halt-key! ::datasource [_ datasource]
  (hcp/close-datasource datasource))

(defmethod ig/init-key ::db-fns [_ _]
  (db/def-db-fns))

(defmethod ig/init-key ::handler [_ config]
  (app config))

(defmethod ig/init-key ::migrations [_ {:keys [datasource migration-dir]}]
  (migratus/migrate {:store :database
                     :migration-dir migration-dir
                     :db {:datasource datasource}}))

(defmethod ig/init-key ::server [_ {:keys [handler port]}]
  (http-kit/run-server handler {:port port :join? false}))

(defmethod ig/halt-key! ::server [_ server]
  (server))

(defn -main [& _]
  (ig/init system-config))
