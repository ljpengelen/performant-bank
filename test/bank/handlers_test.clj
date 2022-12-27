(ns bank.handlers-test
  (:require [bank.domain :refer [make-log-entry]]
            [clojure.test :refer [deftest is testing]]))

(deftest make-log-entry-test
  (testing "Returns entry for withdrawal"
    (is (=
         {:credit 1
          :description "withdraw"
          :sequence 2}
         (make-log-entry 123 {:credit_account_number 123
                              :debit_account_number nil
                              :amount 1
                              :transaction_number 2}))))
  (testing "Returns entry for deposit"
    (is (=
         {:debit 3
          :description "deposit"
          :sequence 4}
         (make-log-entry 123 {:credit_account_number nil
                              :debit_account_number 123
                              :amount 3
                              :transaction_number 4}))))
  (testing "Returns entry for outgoing transaction"
    (is (=
         {:credit 5
          :description "send to #456"
          :sequence 6}
         (make-log-entry 123 {:credit_account_number 123
                              :debit_account_number 456
                              :amount 5
                              :transaction_number 6}))))
  (testing "Returns entry for incoming transaction"
    (is (=
         {:debit 7
          :description "receive from #789"
          :sequence 8}
         (make-log-entry 123 {:credit_account_number 789
                              :debit_account_number 123
                              :amount 7
                              :transaction_number 8})))))
