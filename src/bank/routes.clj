(ns bank.routes
  (:require [bank.handlers :as h]
            [jsonista.core :refer [keyword-keys-object-mapper read-value
                                   write-value-as-string]]
            [reitit.coercion.spec]
            [reitit.ring :as ring]
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

(defn jsonize [handler]
  (fn [{:keys [body] :as request}]
    (let [response (handler (assoc request :body (read-value body keyword-keys-object-mapper)))]
      (-> response
          (assoc :body (write-value-as-string (:body response)))
          (update :headers assoc "Content-type" "application/json")))))

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
            :middleware [[inject :datasource datasource]
                         wrap-params
                         wrap-keyword-params
                         jsonize
                         no-caching]}})
   (ring/routes
    (swagger-ui/create-swagger-ui-handler
     {:path "/api-docs"})
    (ring/create-default-handler
     {:not-found (constantly {:status 404 :body "Not found"})}))))
