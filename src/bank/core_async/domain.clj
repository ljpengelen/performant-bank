(ns bank.core-async.domain
  (:require [bank.core-async.macros :refer [<? with-channel]]
            [bank.db :as db]
            [bank.domain :as domain]
            [clojure.core.async :refer [put!] :as async]))

(def bank
  (reify domain/Bank
    (create-account! [_ datasource name]
      (with-channel [account-chan]
        (put! account-chan (db/create-account! datasource {:name name}))))

    (get-account [_ datasource account-number]
      (with-channel [account-chan]
        (let [account (db/get-account datasource {:account-number account-number})]
          (if account
            (put! account-chan account)
            (put! account-chan (ex-info "Account not found" {:status 404
                                                             :body {:message "Account not found"}}))))))

    (post-deposit! [this datasource account-number amount]
      (with-channel [account-chan]
        (let [amount (domain/valid-amount amount)
              _account (<? (domain/get-account this datasource account-number))
              {:keys [debit-account]} (<? (domain/persist-transaction! datasource nil account-number amount))]
          (put! account-chan debit-account))))

    (make-withdrawal! [this datasource account-number amount]
      (with-channel [account-chan]
        (let [amount (domain/valid-amount amount)
              _account (<? (domain/get-account this datasource account-number))
              {:keys [credit-account]} (<? (domain/persist-transaction! datasource account-number nil amount))]
          (put! account-chan credit-account))))

    (make-transfer! [this datasource credit-account-number debit-account-number amount]
      (with-channel [account-chan]
        (let [amount (domain/valid-amount amount)
              _source-account (<? (domain/get-account this datasource credit-account-number))
              _taget-account (<? (domain/get-account this datasource debit-account-number))
              {:keys [credit-account]} (<? (domain/persist-transaction! datasource credit-account-number debit-account-number amount))]
          (put! account-chan credit-account))))

    (get-transactions [_ datasource account-number]
      (with-channel [transactions-chan]
        (put! transactions-chan (db/get-transactions datasource {:account-number account-number}))))

    (audit-log [this datasource account-number]
      (with-channel [audit-log-chan]
        (let [_account (<? (domain/get-account this datasource account-number))
              transactions (<? (domain/get-transactions this datasource account-number))]
          (put! audit-log-chan (map #(domain/make-log-entry account-number %) transactions)))))))
