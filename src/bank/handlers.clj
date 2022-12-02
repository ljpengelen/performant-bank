(ns bank.handlers 
  (:require [bank.db :as db]
            [clojure.core.async :refer [<! chan go put!] :as async]
            [next.jdbc :refer [with-transaction]]
            [ring.util.response :as rr]))

(defn throw-err [v]
  (if (instance? java.lang.Throwable v) (throw v) v))

(defmacro <? [c]
  `(throw-err (async/<! ~c)))

(defn create-account-chan [datasource name]
  (let [account-chan (chan)]
    (put! account-chan (db/create-account! datasource {:name name}))
    account-chan))

(defn create-account! [request respond _raise]
  (go
    (let [name (-> request :body :name)
          datasource (:datasource request)
          account (<! (create-account-chan datasource name))]
      (respond (rr/response account)))))

(defn get-account-chan [datasource account-number]
  (let [account-chan (chan)]
    (let [account (db/get-account datasource {:account-number account-number})]
      (if account
        (put! account-chan account)
        (put! account-chan (ex-info "Account not found" {:status 404
                                                         :body {:message "Account not found"}}))))
    account-chan))

(defn get-account [request respond _raise]
  (go
    (try
      (let [account-number (-> request :path-params :account-number parse-long)
            datasource (:datasource request)
            account (<? (get-account-chan datasource account-number))]
        (respond (rr/response account)))
      (catch Exception e
        (respond (ex-data e))))))

(defn valid-amount [amount]
  (if (<= amount 0)
    (throw (ex-info "Invalid amount" {:status 400
                                      :body {:message "Amount must be positive"}}))
    amount))

(defn post-deposit-chan [datasource account-number amount]
  (let [account-chan (chan)]
    (with-transaction [tx datasource] 
      (db/persist-transaction! tx {:credit-account-number nil
                                   :debit-account-number account-number
                                   :amount amount})
      (let [account (db/update-balance! tx {:account-number account-number
                                            :amount amount})]
        (put! account-chan account)))
    account-chan))

(defn post-deposit! [request respond _raise]
  (go
    (try
      (let [amount (valid-amount (-> request :body :amount))
            account-number (-> request :path-params :account-number parse-long)
            datasource (:datasource request)
            _account (<? (get-account-chan datasource account-number))
            updated-account (<? (post-deposit-chan datasource account-number amount))]
        (respond (rr/response updated-account)))
      (catch Exception e
        (respond (ex-data e))))))

(defn make-withdrawal-chan [datasource account-number amount]
   (let [account-chan (chan)]
     (with-transaction [tx datasource]
       (db/persist-transaction! tx {:credit-account-number account-number
                                    :debit-account-number nil
                                    :amount amount})
       (if-let [account (db/update-balance! tx {:account-number account-number
                                                :amount (- amount)})]
         (put! account-chan account)
         (put! account-chan (ex-info "Account balance cannot fall below zero" {:status 400
                                                                               :body {:message "Account balance cannot fall below zero"}}))))
     account-chan))

(defn make-withdrawal! [request respond _raise]
  (go
    (try
      (let [amount (valid-amount (-> request :body :amount))
            account-number (-> request :path-params :account-number parse-long)
            datasource (:datasource request)
            _account (<? (get-account-chan datasource account-number))
            updated-account (<? (make-withdrawal-chan datasource account-number amount))]
        (respond (rr/response updated-account)))
      (catch Exception e
        (respond (ex-data e))))))

(defn make-transfer-chan [datasource credit-account-number debit-account-number amount]
  (let [account-chan (chan)]
    (with-transaction [tx datasource]
      (if-let [account (db/update-balance! tx {:account-number credit-account-number
                                               :amount (- amount)})]
        (do
          (db/persist-transaction! tx {:credit-account-number credit-account-number
                                       :debit-account-number debit-account-number
                                       :amount amount})
          (db/update-balance! tx {:account-number debit-account-number
                                  :amount amount})
          (put! account-chan account))
        (put! account-chan (ex-info "Account balance cannot fall below zero" {:status 400
                                                                              :body {:message "Account balance cannot fall below zero"}}))))
    account-chan))

(defn make-transfer! [request respond _raise]
  (go
    (try
      (let [amount (valid-amount (-> request :body :amount))
            credit-account-number (-> request :path-params :account-number parse-long)
            debit-account-number (-> request :body :account-number)
            datasource (:datasource request)
            _source-account (<? (get-account-chan datasource credit-account-number))
            _taget-account (<? (get-account-chan datasource debit-account-number))
            updated-source-account (<? (make-transfer-chan datasource credit-account-number debit-account-number amount))]
        (respond (rr/response updated-source-account)))
      (catch Exception e
        (respond (ex-data e))))))

(defn make-log-entry [account-number {:keys [transaction_number
                                             credit_account_number
                                             debit_account_number
                                             amount]}]
  (-> (cond
       (nil? credit_account_number) {:debit amount
                                     :description "deposit"}
       (nil? debit_account_number) {:credit amount
                                    :description "withdraw"}
       (= account-number credit_account_number) {:credit amount
                                                 :description (str "send to #" debit_account_number)}
       (= account-number debit_account_number) {:debit amount
                                                :description (str "receive from #" credit_account_number)})
      (assoc :sequence transaction_number)))

(defn get-transactions-chan [datasource account-number]
  (let [transactions-chan (chan)]
    (put! transactions-chan (db/get-transactions datasource {:account-number account-number}))
    transactions-chan))

(defn audit-log [request respond _raise]
  (go
    (try
      (let [account-number (-> request :path-params :account-number parse-long)
            datasource (:datasource request)
            _account (<? (get-account-chan datasource account-number))
            transactions (<? (get-transactions-chan datasource account-number))]
        (respond (rr/response (map #(make-log-entry account-number %) transactions))))
      (catch Exception e
        (respond (ex-data e))))))
