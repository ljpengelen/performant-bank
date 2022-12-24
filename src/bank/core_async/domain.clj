(ns bank.core-async.domain
  (:require [bank.core-async.macros :refer [<? with-channel]]
            [bank.db :as db]
            [clojure.core.async :refer [put!] :as async]
            [next.jdbc :refer [with-transaction]]))

(defn create-account! [datasource name]
  (with-channel [account-chan]
    (put! account-chan (db/create-account! datasource {:name name}))))

(defn get-account [datasource account-number]
  (with-channel [account-chan]
    (let [account (db/get-account datasource {:account-number account-number})]
      (if account
        (put! account-chan account)
        (put! account-chan (ex-info "Account not found" {:status 404
                                                         :body {:message "Account not found"}}))))))

(defn valid-amount [amount]
  (if (<= amount 0)
    (throw (ex-info "Invalid amount" {:status 400
                                      :body {:message "Amount must be positive"}}))
    amount))

(defn make-transaction! [datasource credit-account-number debit-account-number amount]
  (with-channel [accounts-chan]
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
                                 :debit-account updated-debit-account})))))))

(defn post-deposit! [datasource account-number amount]
  (with-channel [account-chan]
    (let [amount (valid-amount amount)
          _account (<? (get-account datasource account-number))
          {:keys [debit-account]} (<? (make-transaction! datasource nil account-number amount))]
      (put! account-chan debit-account))))

(defn make-withdrawal! [datasource account-number amount]
  (with-channel [account-chan]
    (let [amount (valid-amount amount)
          _account (<? (get-account datasource account-number))
          {:keys [credit-account]} (<? (make-transaction! datasource account-number nil amount))]
      (put! account-chan credit-account))))

(defn make-transfer! [datasource credit-account-number debit-account-number amount]
  (with-channel [account-chan]
    (let [amount (valid-amount amount)
          _source-account (<? (get-account datasource credit-account-number))
          _taget-account (<? (get-account datasource debit-account-number))
          {:keys [credit-account]} (<? (make-transaction! datasource credit-account-number debit-account-number amount))]
      (put! account-chan credit-account))))

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
  (with-channel [transactions-chan]
    (put! transactions-chan (db/get-transactions datasource {:account-number account-number}))))

(defn audit-log [datasource account-number]
  (with-channel [audit-log-chan]
    (let [_account (<? (get-account datasource account-number))
          transactions (<? (get-transactions datasource account-number))]
      (put! audit-log-chan (map #(make-log-entry account-number %) transactions)))))
