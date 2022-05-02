(ns bank.core-test
  (:require [bank.core :refer [system-config]]
            [clojure.data.json :as json]
            [clojure.test :refer [deftest is testing use-fixtures]]
            [integrant.core :as ig]
            [org.httpkit.client :as client]))

(def test-system-config
  (-> system-config
      (assoc-in [:bank.core/datasource :jdbc-url] "jdbc:sqlite:bank-test.db")
      (assoc-in [:bank.core/server :port] 3001)))

(defn with-running-system [f]
  (let [test-system (ig/init test-system-config)]
    (try
      (f)
      (finally
        (ig/halt! test-system)))))

(use-fixtures :each with-running-system)

(defn http-request
  ([method endpoint] (http-request method endpoint nil))
  ([method endpoint body]
   (-> @(client/request {:url (str "http://localhost:3001/" endpoint)
                         :method method
                         :headers {"Content-type" "application/json"}
                         :body (when body (json/write-str body))})
       :body
       (json/read-str :key-fn keyword))))

(defn uuid []
  (str (java.util.UUID/randomUUID)))

(deftest create_account_integration_test
  (testing "Creates account"
    (let [name (uuid)
          body (http-request :post "account" {:name name})]
      (is (= name (:name body)))
      (is (= 0 (:balance body))))))

(deftest view_account_integration_test
  (testing "Returns account"
    (let [name (uuid)
          create-body (http-request :post "account" {:name name})
          account-number (:account-number create-body)
          view-body (http-request :get (str "account/" account-number))]
      (is (= name (:name view-body)))
      (is (= 0 (:balance view-body)))
      (is (= account-number (:account-number view-body))))))
