(ns bank.domain
  (:require [bank.async :refer [<?]]
            [bank.db :as db]
            [clojure.core.async :refer [chan go put!] :as async]
            [next.jdbc :refer [with-transaction]]))

(defn create-account! [datasource name]
  (let [account-chan (chan)]
    (go
      (put! account-chan (db/create-account! datasource {:name name})))
    account-chan))

(defn get-account [datasource account-number]
  (let [account-chan (chan)]
    (go
      (let [account (db/get-account datasource {:account-number account-number})]
        (if account
          (put! account-chan account)
          (put! account-chan (ex-info "Account not found" {:status 404
                                                           :body {:message "Account not found"}})))))
    account-chan))

(defn valid-amount [amount]
  (if (<= amount 0)
    (throw (ex-info "Invalid amount" {:status 400
                                      :body {:message "Amount must be positive"}}))
    amount))

(defn make-transaction! [datasource credit-account-number debit-account-number amount]
  (let [accounts-chan (chan)]
    (go
      (with-transaction [tx datasource]
        (let [updated-credit-account (when credit-account-number
                                       (db/update-balance! tx {:account-number credit-account-number
                                                               :amount (- amount)}))]
          (if (and credit-account-number (not updated-credit-account))
            (put! accounts-chan (ex-info "Account balance cannot fall below zero" {:status 400
                                                                                   :body {:message "Account balance cannot fall below zero"}}))
            (let [updated-debit-account (when debit-account-number
                                          (db/update-balance! tx {:account-number debit-account-number
                                                                  :amount amount}))]
              (db/persist-transaction! tx {:credit-account-number credit-account-number
                                           :debit-account-number debit-account-number
                                           :amount amount})
              (put! accounts-chan {:credit-account updated-credit-account
                                   :debit-account updated-debit-account}))))))
    accounts-chan))

(defn post-deposit! [datasource account-number amount]
  (let [account-chan (chan)]
    (go
      (try
        (let [amount (valid-amount amount)
              _account (<? (get-account datasource account-number))
              {:keys [debit-account]} (<? (make-transaction! datasource nil account-number amount))]
          (put! account-chan debit-account))
        (catch Exception e
          (put! account-chan e))))
    account-chan))

(defn make-withdrawal! [datasource account-number amount]
  (let [account-chan (chan)]
    (go
      (try
        (let [amount (valid-amount amount)
              _account (<? (get-account datasource account-number))
              {:keys [credit-account]} (<? (make-transaction! datasource account-number nil amount))]
          (put! account-chan credit-account))
        (catch Exception e
          (put! account-chan e))))
    account-chan))

(defn make-transfer! [datasource credit-account-number debit-account-number amount]
  (let [account-chan (chan)]
    (go
      (try
        (let [amount (valid-amount amount)
              _source-account (<? (get-account datasource credit-account-number))
              _taget-account (<? (get-account datasource debit-account-number))
              {:keys [credit-account]} (<? (make-transaction! datasource credit-account-number debit-account-number amount))]
          (put! account-chan credit-account))
        (catch Exception e
          (put! account-chan e))))
    account-chan))

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

(defn get-transactions [datasource account-number]
  (let [transactions-chan (chan)]
    (go
      (put! transactions-chan (db/get-transactions datasource {:account-number account-number})))
    transactions-chan))

(defn audit-log [datasource account-number]
  (let [audit-log-chan (chan)]
     (go
       (try
         (let [_account (<? (get-account datasource account-number))
               transactions (<? (get-transactions datasource account-number))]
           (put! audit-log-chan (map #(make-log-entry account-number %) transactions)))
         (catch Exception e
           (put! audit-log-chan e))))
    audit-log-chan))
