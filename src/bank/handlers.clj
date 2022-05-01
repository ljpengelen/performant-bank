(ns bank.handlers 
  (:require [bank.db :as db]
            [ring.util.response :as rr]))

(defn handler [request]
  (let [name (-> request :parameters :body :name)]
    (rr/response (first (db/create-account! @db/datasource {:name name})))))
