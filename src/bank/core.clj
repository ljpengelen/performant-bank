(ns bank.core
  (:require [bank.routes :refer [app]]
            [config.core :refer [env]]
            [hikari-cp.core :as hcp]
            [integrant.core :as ig]
            [migratus.core :as migratus]
            [org.httpkit.server :as http-kit])
  (:gen-class))

(def system-config
  {::datasource {:jdbc-url (-> env :db :jdbc-url)}
   ::migrations {:datasource (ig/ref ::datasource)}
   ::server {:datasource (ig/ref ::datasource)
             :migrations (ig/ref ::migrations)}})

(defmethod ig/init-key ::datasource [_ {:keys [jdbc-url]}]
  (hcp/make-datasource {:jdbc-url jdbc-url}))

(defmethod ig/halt-key! ::datasource [_ datasource]
  (hcp/close-datasource datasource))

(defmethod ig/init-key ::migrations [_ {:keys [datasource]}]
  (migratus/migrate {:store :database
                     :db {:datasource datasource}}))

(defmethod ig/init-key ::server [_ {:keys [datasource]}]
  (http-kit/run-server (app datasource) {:port 3000 :join? false}))

(defmethod ig/halt-key! ::server [_ server]
  (server))

(defn -main [_]
  (ig/init system-config))
