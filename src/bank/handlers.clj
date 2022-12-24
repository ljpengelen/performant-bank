(ns bank.handlers 
  (:require [bank.domain :as domain]))

(defn create-account! [{:keys [bank datasource]} request]
  (let [name (-> request :body :name)]
    (domain/create-account! bank datasource name)))

(defn get-account [{:keys [bank datasource]} request]
  (let [account-number (-> request :path-params :account-number parse-long)]
    (domain/get-account bank datasource account-number)))

(defn post-deposit! [{:keys [bank datasource]} request]
  (let [amount (-> request :body :amount)
        account-number (-> request :path-params :account-number parse-long)]
    (domain/post-deposit! bank datasource account-number amount)))

(defn make-withdrawal! [{:keys [bank datasource]} request]
  (let [amount (-> request :body :amount)
        account-number (-> request :path-params :account-number parse-long)]
    (domain/make-withdrawal! bank datasource account-number amount)))

(defn make-transfer! [{:keys [bank datasource]} request]
  (let [amount (-> request :body :amount)
        credit-account-number (-> request :path-params :account-number parse-long)
        debit-account-number (-> request :body :account-number)]
    (domain/make-transfer! bank datasource credit-account-number debit-account-number amount)))

(defn audit-log [{:keys [bank datasource]} request]
  (let [account-number (-> request :path-params :account-number parse-long)]
    (domain/audit-log bank datasource account-number)))
