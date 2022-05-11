(ns bank.handlers 
  (:require [bank.db :as db]
            [next.jdbc :refer [with-transaction]]
            [ring.util.response :as rr]))

(defn create-account!
  ([request respond _raise]
   (let [name (-> request :body :name)
         datasource (:datasource request)]
     (respond (rr/response (db/create-account! datasource {:name name}))))))

(defn get-account [request respond _raise]
  (let [account-number (-> request :path-params :account-number parse-long)
        datasource (:datasource request)]
    (if-let [account (db/get-account datasource {:account-number account-number})]
      (respond (rr/response account))
      (respond (rr/not-found {:message "Account not found"})))))

(defn post-deposit! [request respond _raise]
  (let [amount (-> request :body :amount)]
    (if (<= amount 0)
      (respond (rr/bad-request {:message "Amount must be positive"}))
      (let [account-number (-> request :path-params :account-number parse-long)
            datasource (:datasource request)]
        (with-transaction [tx datasource]
          (if-let [account (db/get-account tx {:account-number account-number})]
            (do
              (db/persist-transaction! tx {:credit-account-number nil
                                           :debit-account-number account-number
                                           :amount amount})
              (respond (rr/response (db/set-balance! tx {:account-number account-number
                                                         :balance (+ amount (:balance account))}))))
            (respond (rr/bad-request {:message "Account does not exist"}))))))))

(defn make-withdrawal! [request respond _raise]
  (let [amount (-> request :body :amount)]
    (if (<= amount 0)
      (respond (rr/bad-request {:message "Amount must be positive"}))
      (let [account-number (-> request :path-params :account-number parse-long)
            datasource (:datasource request)]
        (with-transaction [tx datasource]
          (if-let [account (db/get-account tx {:account-number account-number})]
            (if (>= (:balance account) amount)
              (do
                (db/persist-transaction! tx {:credit-account-number account-number
                                             :debit-account-number nil
                                             :amount amount})
                (respond (rr/response (db/set-balance! tx {:account-number account-number
                                                           :balance (- (:balance account) amount)}))))
              (respond (rr/bad-request {:message "Account balance cannot fall below zero"})))
            (respond (rr/bad-request {:message "Account does not exist"}))))))))

(defn make-transfer! [request respond _raise]
  (let [amount (-> request :body :amount)]
    (if (<= amount 0)
      (respond (rr/bad-request {:message "Amount must be positive"}))
      (let [credit-account-number (-> request :path-params :account-number parse-long)
            debit-account-number (-> request :body :account-number)
            datasource (:datasource request)]
        (with-transaction [tx datasource]
          (let [source-account (db/get-account tx {:account-number credit-account-number})
                target-account (db/get-account tx {:account-number debit-account-number})]
            (if (and source-account target-account)
              (if (>= (:balance source-account) amount)
                (do
                  (db/persist-transaction! tx {:credit-account-number credit-account-number
                                               :debit-account-number debit-account-number
                                               :amount amount})
                  (db/set-balance! tx {:account-number debit-account-number
                                       :balance (+ (:balance target-account) amount)})
                  (respond (rr/response (db/set-balance! tx {:account-number credit-account-number
                                                             :balance (- (:balance source-account) amount)}))))
                (respond (rr/bad-request {:message "Account balance cannot fall below zero"})))
              (respond (rr/bad-request {:message "Account does not exist"})))))))))

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

(defn audit-log [request respond _raise]
  (let [account-number (-> request :path-params :account-number parse-long)
        datasource (:datasource request)]
    (if-let [transactions (db/get-transactions datasource {:account-number account-number})]
      (respond (rr/response (map #(make-log-entry account-number %) transactions)))
      (respond (rr/not-found {:message "Account not found"})))))
