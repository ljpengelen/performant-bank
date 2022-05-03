(ns benchmark
  (:require [criterium.core :refer [quick-bench]]
            [org.httpkit.client :as client]))

(defn http-request
  ([method endpoint] (http-request method endpoint nil))
  ([method endpoint body]
   @(client/request {:url (str "http://localhost:3000/" endpoint)
                     :method method
                     :headers {"Content-type" "application/json"}
                     :body body})))

(comment
  ;; Execution time mean : 87,820996 µs
  (quick-bench (http-request :post "account" "{\"name\": \"Benchmark\"}"))
  ;; Execution time mean : 88,185795 µs
  (quick-bench (http-request :get "account/1"))
  ;; Execution time mean : 90,140511 µs
  (quick-bench (http-request :post "account/1/deposit" "{\"amount\": 10}"))
  ;; Execution time mean : 90,270936 µs
  (quick-bench (http-request :post "account/1/withdraw" "{\"amount\": 1}"))
  ;; Execution time mean : 89,329678 µs
  (quick-bench (http-request :post "account/1/send" "{\"account-number\": 2, \"amount\": 1}"))
  ;; Execution time mean : 88,261390 µs
  (quick-bench (http-request :get "account/1/audit"))
  ;; Execution time mean : 90,320477 µs
  (quick-bench (http-request :get "account/3/audit")))
