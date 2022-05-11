(ns bank.routes
  (:require [bank.handlers :as h]
            [jsonista.core :refer [keyword-keys-object-mapper read-value
                                   write-value-as-string]]
            [org.httpkit.server :refer [as-channel send!]]
            [reitit.coercion.spec]
            [reitit.ring :as ring]
            [reitit.swagger :as swagger]
            [reitit.swagger-ui :as swagger-ui]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.params :refer [wrap-params]]))

(defn respond-async [channel]
  (fn [response]
    (send! channel response)))

(defn raise-async [channel]
  (fn [error]
    (println error)
    (send! channel {:status 500 :body "Internal server error"})))

(defn wrap-async [handler]
  (fn [request]
    (as-channel
     request
     {:on-open (fn [channel]
                 (handler
                  request
                  (respond-async channel)
                  (raise-async channel)))})))

(defn no-caching-response [response]
  (assoc-in response [:headers "Cache-Control"] "no-cache, no-store"))

(defn wrap-no-caching [handler]
  (fn
    ([request]
     (no-caching-response (handler request)))
    ([request respond raise]
     (handler request (comp respond no-caching-response) raise))))

(defn inject-request [request key dependency]
  (assoc request key dependency))

(defn wrap-inject [handler key dependency]
  (fn
    ([request]
     (handler (inject-request request key dependency)))
    ([request respond raise]
     (handler (inject-request request key dependency) respond raise))))

(defn json-request [{:keys [body] :as request}]
  (assoc request :body (read-value body keyword-keys-object-mapper)))

(defn wrap-json-request [handler]
  (fn
    ([request]
     (handler (json-request request)))
    ([request respond raise]
     (handler (json-request request) respond raise))))

(defn json-response [response]
  (-> response
      (assoc :body (write-value-as-string (:body response)))
      (update :headers assoc "Content-type" "application/json")))

(defn wrap-json-response [handler]
  (fn
    ([request]
     (json-response (handler request)))
    ([request respond raise]
     (handler request (comp respond json-response) raise))))

(defn app [datasource]
  (ring/ring-handler
   (ring/router
    [["/account"
      ["" {:post {:handler h/create-account!
                  :parameters {:body {:name string?}}
                  :summary "Create a new bank account."
                  :responses {200 {:account-number int?
                                   :name string?
                                   :balance int?}}}}]
      ["/:account-number"
       ["" {:get {:handler h/get-account}
            :parameters {:path {:account-number int?}}
            :summary "Get a bank account by its number."
            :responses {200 {:account-number int?
                             :name string?
                             :balance int?}
                        404 {:message string?}}}]
       ["/deposit" {:post {:handler h/post-deposit!}
                    :parameters {:path {:account-number int?}
                                 :body {:amount int?}}
                    :summary "Post a deposit to the bank account with the given number."
                    :responses {200 {:account-number int?
                                     :name string?
                                     :balance int?}}}]
       ["/withdraw" {:post {:handler h/make-withdrawal!}
                    :parameters {:path {:account-number int?}
                                 :body {:amount int?}}
                    :summary "Make a withdrawal from the bank account with the given number."
                    :responses {200 {:account-number int?
                                     :name string?
                                     :balance int?}}}]
       ["/send" {:post {:handler h/make-transfer!}
                 :parameters {:path {:account-number int?}
                              :body {:amount int?
                                     :account-number int?}}
                 :summary "Transfer money from the bank account in the path to the one in the body."
                 :responses {200 {:account-number int?
                                  :name string?
                                  :balance int?}}}]
       ["/audit" {:get {:handler h/audit-log}
                  :parameters {:path {:account-number int?}}
                  :summary "Get all transactions for an account"
                  :responses {200 {:sequence int?
                                   :debit int?
                                   :credit int?
                                   :description string?}}}]]]
     ["/swagger.json" {:get {:no-doc true
                             :swagger {:info {:title "Bank account management API"}}
                             :handler (swagger/create-swagger-handler)}}]]
    {:data {:coercion reitit.coercion.spec/coercion
            :middleware [wrap-async
                         [wrap-inject :datasource datasource]
                         wrap-params
                         wrap-keyword-params
                         wrap-json-request
                         wrap-json-response
                         wrap-no-caching]}})
   (ring/routes
    (swagger-ui/create-swagger-ui-handler
     {:path "/api-docs"})
    (ring/create-default-handler
     {:not-found (constantly {:status 404 :body "Not found"})}))))
