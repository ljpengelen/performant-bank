(ns bank.handlers 
  (:require [bank.domain :as domain]))

(defn create-account! [{:keys [datasource]} request]
  (let [name (-> request :body :name)]
    (domain/create-account! datasource name)))

(defn get-account [{:keys [datasource]} request]
  (let [account-number (-> request :path-params :account-number parse-long)]
    (domain/get-account datasource account-number)))

(defn post-deposit! [{:keys [datasource]} request]
  (let [amount (-> request :body :amount)
        account-number (-> request :path-params :account-number parse-long)]
    (domain/post-deposit! datasource account-number amount)))

(defn make-withdrawal! [{:keys [datasource]} request]
  (let [amount (-> request :body :amount)
        account-number (-> request :path-params :account-number parse-long)]
    (domain/make-withdrawal! datasource account-number amount)))

(defn make-transfer! [{:keys [datasource]} request]
  (let [amount (-> request :body :amount)
        credit-account-number (-> request :path-params :account-number parse-long)
        debit-account-number (-> request :body :account-number)]
    (domain/make-transfer! datasource credit-account-number debit-account-number amount)))

(defn audit-log [{:keys [datasource]} request]
  (let [account-number (-> request :path-params :account-number parse-long)]
    (domain/audit-log datasource account-number)))
