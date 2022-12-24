(ns bank.manifold.middleware
  (:require [manifold.deferred :as d]
            [ring.util.response :as response]))

(defn async-response [response-deferred]
  (-> response-deferred
      (d/chain (fn [d] (response/response d)))
      (d/catch Exception (fn [e] (response/response (ex-data e))))))

(defn wrap-async [handler]
  (fn
    ([request] 
     @(async-response (handler request)))
    ([request respond raise]
     (d/on-realized
      (async-response (handler request))
      respond
      raise))))
