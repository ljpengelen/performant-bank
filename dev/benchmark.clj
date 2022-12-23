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
  ;; http-kit - Execution time mean : 1,539716 ms
  ;; Jetty sync - Execution time mean : 1,034205 ms
  ;; Jetty async - Execution time mean : 1,148571 ms
  ;; Jetty 11 sync - Execution time mean : 1,532254 ms
  ;; Jetty 11 async - Execution time mean : 1,893996 ms
  (quick-bench (http-request :post "account" "{\"name\": \"Benchmark\"}"))

  ;; http-kit - Execution time mean : 277,740135 µs
  ;; Jetty sync - Execution time mean : 221,813513 µs
  ;; Jetty async - Execution time mean : 221,346009 µs
  ;; Jetty 11 sync - Execution time mean : 265,358132 µs
  ;; Jetty 11 async - Execution time mean : 354,104871 µs
  (quick-bench (http-request :get "account/1"))

  ;; http-kit - Execution time mean : 1,477777 ms
  ;; Jetty sync - Execution time mean : 973,245678 µs
  ;; Jetty async - Execution time mean : 998,309678 µs
  ;; Jetty 11 sync - Execution time mean : 2,540063 ms
  ;; Jetty 11 async - Execution time mean : 1,557065 ms
  (quick-bench (http-request :post "account/1/deposit" "{\"amount\": 10}"))

  ;; http-kit - Execution time mean : 1,431105 ms
  ;; Jetty sync - Execution time mean : 1,001110 ms
  ;; Jetty async - Execution time mean : 1,303762 ms
  ;; Jetty 11 sync - Execution time mean : 1,396568 ms
  ;; Jetty 11 async - Execution time mean : 2,082553 ms
  (quick-bench (http-request :post "account/1/withdraw" "{\"amount\": 1}"))

  ;; http-kit - Execution time mean : 1,733032 ms
  ;; Jetty sync - Execution time mean : 1,118040 ms
  ;; Jetty async - Execution time mean : 1,130116 ms
  ;; Jetty 11 sync - Execution time mean : 1,543914 ms
  ;; Jetty 11 async - Execution time mean : 1,679548 ms
  (quick-bench (http-request :post "account/1/send" "{\"account-number\": 2, \"amount\": 1}"))

  ;; http-kit - Execution time mean : 2,442319 ms
  ;; Jetty sync - Execution time mean : 6,679884 ms
  ;; Jetty async - Execution time mean : 3,318397 ms
  ;; Jetty 11 sync - Execution time mean : 3,338426 ms
  ;; Jetty 11 async - Execution time mean : 6,899944 ms
  (quick-bench (http-request :get "account/3/audit")))
