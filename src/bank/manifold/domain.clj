(ns bank.manifold.domain
  (:require [bank.db :as db]
            [bank.domain :as domain]
            [manifold.deferred :as d]))

(def bank
  (reify domain/Bank
    (create-account! [_ datasource name]
      (d/future (db/create-account! datasource {:name name})))

    (get-account [_ datasource account-number]
      (d/future
        (if-let [account (db/get-account datasource {:account-number account-number})]
          account
          (throw (ex-info "Account not found" {:status 404
                                               :body {:message "Account not found"}})))))

    (post-deposit! [this datasource account-number amount]
      (d/let-flow [amount (d/future (domain/valid-amount amount))
                   _account (domain/get-account this datasource account-number)
                   {:keys [debit-account]} (domain/persist-transaction! datasource nil account-number amount)]
                  debit-account))

    (make-withdrawal! [this datasource account-number amount]
      (d/let-flow [amount (d/future (domain/valid-amount amount))
                   _account (domain/get-account this datasource account-number)
                   {:keys [credit-account]} (domain/persist-transaction! datasource account-number nil amount)]
                  credit-account))

    (make-transfer! [this datasource credit-account-number debit-account-number amount]
      (d/let-flow [amount (d/future (domain/valid-amount amount))
                   _source-account (domain/get-account this datasource credit-account-number)
                   _taget-account (domain/get-account this datasource debit-account-number)
                   {:keys [credit-account]} (domain/persist-transaction! datasource credit-account-number debit-account-number amount)]
                  credit-account))

    (get-transactions [_ datasource account-number]
      (d/future (db/get-transactions datasource {:account-number account-number})))

    (audit-log [this datasource account-number]
      (d/let-flow [_account (domain/get-account this datasource account-number)
                   transactions (domain/get-transactions this datasource account-number)]
                  (map #(domain/make-log-entry account-number %) transactions)))))
