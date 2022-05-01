(ns bank.core
  (:require [bank.routes :refer [app]]
            [clojure.java.browse :refer [browse-url]]
            [org.httpkit.server :as http-kit]
            [ring.middleware.reload :refer [wrap-reload]])
  (:gen-class))

(defonce server (atom nil))

(defn start! []
  (when-not @server
    (reset! server (http-kit/run-server (wrap-reload #'app) {:port 3000 :join? false}))))

(defn stop! []
  (when-let [running-server @server]
    (running-server)
    (reset! server nil)))

(defn -main [_]
  (start!))

(comment
  (start!)
  (browse-url "http://localhost:3000/api-docs")
  (stop!))
