(ns bank.core
  (:require [bank.db :as db]
            [bank.routes :refer [app]]
            [config.core :refer [env]]
            [hikari-cp.core :as hcp]
            [integrant.core :as ig]
            [migratus.core :as migratus]
            [org.httpkit.server :as http-kit]
            [ring.adapter.jetty :refer [run-jetty]])
  (:gen-class))

(def system-config
  (let [server-type (:server-type env)
        async? (= server-type :jetty-async)]
    {::datasource (get-in env [:db-config (:db env)])
     ::db-fns nil
     ::handler {:async? async?
                :datasource (ig/ref ::datasource)}
     ::migrations {:datasource (ig/ref ::datasource)
                   :migration-dir (get-in env [:db-config (:db env) :migration-dir])}
     ::server {:async? async?
               :db-fns (ig/ref ::db-fns)
               :handler (ig/ref ::handler)
               :migrations (ig/ref ::migrations)
               :port (:port env)
               :server-type server-type}}))

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

(defmethod ig/init-key ::server [_ {:keys [handler port server-type]}]
  (case server-type
    :http-kit (http-kit/run-server handler {:port port
                                            :join? false})
    (:jetty-async :jetty-sync) (let [server (run-jetty handler {:port port
                                                                :join? false
                                                                :async? false})]
                                 (fn [] (.stop server)))
    (throw (ex-info "Invalid configuration: unknown server type" {:server-type server-type}))))

(defmethod ig/halt-key! ::server [_ stop-server]
  (stop-server))

(defn -main [& _]
  (ig/init system-config))
