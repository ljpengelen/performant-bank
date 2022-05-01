(ns bank.routes
  (:require [bank.handlers :as h]
            [muuntaja.core :as m]
            [reitit.coercion.spec]
            [reitit.ring :as ring]
            [reitit.ring.coercion :as coercion]
            [reitit.ring.middleware.muuntaja :as muuntaja]
            [reitit.swagger :as swagger]
            [reitit.swagger-ui :as swagger-ui]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.params :refer [wrap-params]]))

(defn no-caching [handler]
  (fn [request]
    (let [response (handler request)]
      (assoc-in response [:headers "Cache-Control"] "no-cache, no-store"))))

(defn inject [handler key dependency]
  (fn [request]
    (handler (assoc request key dependency))))

(defn app [datasource]
  (ring/ring-handler
   (ring/router
    [["/account" {:post {:handler h/handler
                         :parameters {:body {:name string?}}
                         :summary "Create a new bank account."
                         :responses {200 {:account-number int?
                                          :name string?
                                          :balance int?}}}}]
     ["/swagger.json" {:get {:no-doc true
                             :swagger {:info {:title "Bank account management API"}}
                             :handler (swagger/create-swagger-handler)}}]]
    {:data {:coercion reitit.coercion.spec/coercion
            :middleware [[inject :datasource datasource]
                         wrap-params
                         wrap-keyword-params
                         muuntaja/format-negotiate-middleware
                         muuntaja/format-response-middleware
                         muuntaja/format-request-middleware
                         coercion/coerce-response-middleware
                         coercion/coerce-request-middleware
                         no-caching]
            :muuntaja m/instance}})
   (ring/routes
    (swagger-ui/create-swagger-ui-handler
     {:path "/api-docs"})
    (ring/create-default-handler
     {:not-found (constantly {:status 404 :body "Not found"})}))))
