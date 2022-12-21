(ns bank.routes
  (:require [bank.async :refer [<??]]
            [bank.handlers :as h]
            [jsonista.core :refer [keyword-keys-object-mapper read-value
                                   write-value-as-string]]
            [reitit.coercion.spec]
            [reitit.ring :as ring]
            [reitit.swagger :as swagger]
            [reitit.swagger-ui :as swagger-ui]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.util.response :as response]))

(defn wrap-async [handler]
  (fn [request]
     (try
       (response/response (<?? (handler request)))
       (catch Exception e
         (response/response (ex-data e))))))

(defn no-caching-response [response]
  (assoc-in response [:headers "Cache-Control"] "no-cache, no-store"))

(defn wrap-no-caching [handler]
  (fn
    ([request]
     (no-caching-response (handler request)))
    ([request respond raise]
     (handler request (comp respond no-caching-response) raise))))

(defn wrap-config [handler config]
  (fn
    ([request]
     (handler config request))
    ([request respond raise]
     (handler config request respond raise))))

(defn json-request [{:keys [body request-method] :as request}]
  (if (#{:get} request-method)
    request
    (assoc request :body (read-value body keyword-keys-object-mapper))))

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

(defn app [config]
  (ring/ring-handler
   (ring/router
    [["/account" {:middleware [wrap-async
                               [wrap-config config]]}
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
                             :swagger {:info {:title "Bank-account management API"}}
                             :handler (swagger/create-swagger-handler)}}]]
    {:data {:coercion reitit.coercion.spec/coercion
            :middleware [wrap-no-caching
                         wrap-params
                         wrap-keyword-params
                         wrap-json-request
                         wrap-json-response]}})
   (ring/routes
    (swagger-ui/create-swagger-ui-handler
     {:path "/api-docs"})
    (ring/create-default-handler
     {:not-found (constantly {:status 404 :body "Not found"})}))))
