(ns benchmark
  (:require [clojure.data.json :as json]
            [criterium.core :refer [quick-bench]]
            [org.httpkit.client :as client]))

(defn http-request
  ([method endpoint] (http-request method endpoint nil))
  ([method endpoint body]
   (-> @(client/request {:url (str "http://localhost:3000/" endpoint)
                         :method method
                         :headers {"Content-type" "application/json"}
                         :body (when body (json/write-str body))})
       :body
       (json/read-str :key-fn keyword))))

(comment
  ;; Execution time mean : 1,084466 ms
  (quick-bench (http-request :post "account" {:name "Benchmark"}))
  ;; Execution time mean : 275,773633 µs
  (quick-bench (http-request :get "account/1"))
  ;; Execution time mean : 1,359019 ms
  (quick-bench (http-request :post "account/1/deposit" {:amount 10}))
  ;; Execution time mean : 1,584553 ms
  (quick-bench (http-request :post "account/1/withdraw" {:amount 1}))
  ;; Execution time mean : 1,383149 ms
  (quick-bench (http-request :post "account/1/send" {:account-number 2 :amount 1}))
  ;; Execution time mean : 120,300763 ms
  (quick-bench (http-request :get "account/1/audit")))
