(ns bank.core-async.middleware
  (:require [bank.core-async.macros :refer [<? <??]]
            [clojure.core.async :refer [chan go put!]]
            [ring.util.response :as response]))

(defn async-response [handler-chan]
  (let [response-chan (chan)]
    (go
      (try
        (put! response-chan (response/response (<? handler-chan)))
        (catch Exception e
          (put! response-chan (response/response (ex-data e))))))
    response-chan))

(defn wrap-async [handler]
  (fn
    ([request]
     (<?? (async-response (handler request))))
    ([request respond _raise]
     (go
       (respond (<? (async-response (handler request))))))))
