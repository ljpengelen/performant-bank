(ns bank.core-async.domain
  (:require [bank.core-async.macros :refer [<? with-channel]]
            [bank.db :as db]
            [bank.domain :as domain]
            [clojure.core.async :refer [put!] :as async]))

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

(defn make-transaction! [datasource credit-account-number debit-account-number amount]
  (with-channel [accounts-chan]
    (put! accounts-chan (domain/make-transaction! datasource credit-account-number debit-account-number amount))))

(defn post-deposit! [datasource account-number amount]
  (with-channel [account-chan]
    (let [amount (domain/valid-amount amount)
          _account (<? (get-account datasource account-number))
          {:keys [debit-account]} (<? (make-transaction! datasource nil account-number amount))]
      (put! account-chan debit-account))))

(defn make-withdrawal! [datasource account-number amount]
  (with-channel [account-chan]
    (let [amount (domain/valid-amount amount)
          _account (<? (get-account datasource account-number))
          {:keys [credit-account]} (<? (make-transaction! datasource account-number nil amount))]
      (put! account-chan credit-account))))

(defn make-transfer! [datasource credit-account-number debit-account-number amount]
  (with-channel [account-chan]
    (let [amount (domain/valid-amount amount)
          _source-account (<? (get-account datasource credit-account-number))
          _taget-account (<? (get-account datasource debit-account-number))
          {:keys [credit-account]} (<? (make-transaction! datasource credit-account-number debit-account-number amount))]
      (put! account-chan credit-account))))

(defn get-transactions [datasource account-number]
  (with-channel [transactions-chan]
    (put! transactions-chan (db/get-transactions datasource {:account-number account-number}))))

(defn audit-log [datasource account-number]
  (with-channel [audit-log-chan]
    (let [_account (<? (get-account datasource account-number))
          transactions (<? (get-transactions datasource account-number))]
      (put! audit-log-chan (map #(domain/make-log-entry account-number %) transactions)))))
