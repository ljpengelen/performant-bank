(ns bank.core
  (:require [bank.db :as db]
            [bank.routes :refer [app]]
            [config.core :refer [env]]
            [hikari-cp.core :as hcp]
            [integrant.core :as ig]
            [migratus.core :as migratus]
            [org.httpkit.server :as http-kit]
            [ring.adapter.undertow :refer [run-undertow]])
  (:gen-class))

(defn system-config []
  (let [server-type (:server-type env)
        async? (contains? #{:jetty-async :undertow-async} server-type)]
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

(defn try-requiring-resolve [sym]
  (try
    (requiring-resolve sym)
    (catch Exception _ nil)))

(defn run-jetty [handler options]
  (if-let [run-jetty (or
                      (try-requiring-resolve 'ring.adapter.jetty/run-jetty)
                      (try-requiring-resolve 'ring.adapter.jetty9/run-jetty))]
    (run-jetty handler options)
    (throw (RuntimeException. "Invalid configuration: no Jetty adapter on classpath"))))

(defmethod ig/init-key ::server [_ {:keys [async? handler port server-type]}]
  (println "Starting server " server-type " - async: " async?)
  (case server-type
    :http-kit (http-kit/run-server handler {:port port
                                            :join? false})
    (:jetty-async :jetty-sync) (let [server (run-jetty handler {:port port
                                                                :join? false
                                                                :async? async?})]
                                 (fn [] (.stop server)))
    (:undertow-async :undertow-sync) (let [server (run-undertow handler {:port port
                                                                         :async? async?})]
                                       (fn [] (.stop server)))
    (throw (ex-info "Invalid configuration: unknown server type" {:server-type server-type}))))

(defmethod ig/halt-key! ::server [_ stop-server]
  (stop-server))

(defn -main [& _]
  (ig/init (system-config)))
