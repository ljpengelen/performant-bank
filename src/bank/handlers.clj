(ns bank.handlers 
  (:require [bank.db :as db]
            [next.jdbc :refer [with-transaction]]
            [ring.util.response :as rr]))

(defn create-account! [request]
  (let [name (-> request :parameters :body :name)
        datasource (:datasource request)]
    (rr/response (db/create-account! datasource {:name name}))))

(defn get-account [request]
  (let [account-number (-> request :parameters :path :account-number)
        datasource (:datasource request)]
    (if-let [account (db/get-account datasource {:account-number account-number})]
      (rr/response account)
      (rr/not-found {:message "Account not found"}))))

(defn post-deposit! [request]
  (let [amount (-> request :parameters :body :amount)]
    (if (<= amount 0)
      (rr/bad-request {:message "Amount must be positive"})
      (let [account-number (-> request :parameters :path :account-number)
            datasource (:datasource request)]
        (with-transaction [tx datasource]
          (if-let [account (db/get-account tx {:account-number account-number})]
            (do
              (db/persist-transaction! tx {:source-account-number account-number
                                           :target-account-number nil
                                           :credit nil
                                           :debit amount})
              (rr/response (db/set-balance! tx {:account-number account-number
                                                :balance (+ amount (:balance account))})))
            (rr/bad-request {:message "Account does not exist"})))))))

(defn make-withdrawal! [request]
  (let [amount (-> request :parameters :body :amount)]
    (if (<= amount 0)
      (rr/bad-request {:message "Amount must be positive"})
      (let [account-number (-> request :parameters :path :account-number)
            datasource (:datasource request)]
        (with-transaction [tx datasource]
          (if-let [account (db/get-account tx {:account-number account-number})]
            (if (>= (:balance account) amount)
              (do
                (db/persist-transaction! tx {:source-account-number account-number
                                             :target-account-number nil
                                             :credit amount
                                             :debit nil})
                (rr/response (db/set-balance! tx {:account-number account-number
                                                  :balance (- (:balance account) amount)})))
              (rr/bad-request {:message "Account balance cannot fall below zero"}))
            (rr/bad-request {:message "Account does not exist"})))))))

(defn make-transfer! [request]
  (let [amount (-> request :parameters :body :amount)]
    (if (<= amount 0)
      (rr/bad-request {:message "Amount must be positive"})
      (let [source-account-number (-> request :parameters :path :account-number)
            target-account-number (-> request :parameters :body :account-number)
            datasource (:datasource request)]
        (with-transaction [tx datasource]
          (let [source-account (db/get-account tx {:account-number source-account-number})
                target-account (db/get-account tx {:account-number target-account-number})]
            (if (and source-account target-account)
              (if (>= (:balance source-account) amount)
                (do
                  (db/persist-transaction! tx {:source-account-number source-account-number
                                               :target-account-number target-account-number
                                               :credit amount
                                               :debit nil})
                  (db/set-balance! tx {:account-number target-account-number
                                       :balance (+ (:balance target-account) amount)})
                  (rr/response (db/set-balance! tx {:account-number source-account-number
                                                    :balance (- (:balance source-account) amount)})))
                (rr/bad-request {:message "Account balance cannot fall below zero"}))
              (rr/bad-request {:message "Account does not exist"}))))))))
