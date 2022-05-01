(ns bank.handlers 
  (:require [bank.db :as db]
            [ring.util.response :as rr]))

(defn handler [request]
  (let [name (-> request :parameters :body :name)
        datasource (:datasource request)]
    (rr/response (first (db/create-account! datasource {:name name})))))
