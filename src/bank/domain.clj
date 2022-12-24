(ns bank.domain
  (:require [bank.db :as db]
            [next.jdbc :refer [with-transaction]]))

(defprotocol Bank
  (create-account! [this datasource name])
  (get-account [this datasource account-number])
  (post-deposit! [this datasource account-number amount])
  (make-withdrawal! [this datasource account-number amount])
  (make-transfer! [this datasource credit-account-number debit-account-number amount])
  (get-transactions [this datasource account-number])
  (audit-log [this datasource account-number]))

(defn valid-amount [amount]
  (if (<= amount 0)
    (throw (ex-info "Invalid amount" {:status 400
                                      :body {:message "Amount must be positive"}}))
    amount))

(defn persist-transaction! [datasource credit-account-number debit-account-number amount]
  (with-transaction [tx datasource]
    (let [updated-credit-account (when credit-account-number
                                   (db/update-balance! tx {:account-number credit-account-number
                                                           :amount (- amount)}))]
      (if (and credit-account-number (not updated-credit-account))
        (throw (ex-info "Account balance cannot fall below zero" {:status 400
                                                                  :body {:message "Account balance cannot fall below zero"}}))
        (let [updated-debit-account (when debit-account-number
                                      (db/update-balance! tx {:account-number debit-account-number
                                                              :amount amount}))]
          (db/persist-transaction! tx {:credit-account-number credit-account-number
                                       :debit-account-number debit-account-number
                                       :amount amount})
          {:credit-account updated-credit-account
           :debit-account updated-debit-account})))))

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
