(ns load-test
  (:require [clj-gatling.core :as clj-gatling]
            [clojure.data.json :as json]
            [org.httpkit.client :as client]))

(defn http-request
  ([method endpoint] (http-request method endpoint nil))
  ([method endpoint body]
   (let [response @(client/request {:url (str "http://localhost:3000/" endpoint)
                                    :method method
                                    :headers {"Content-type" "application/json"}
                                    :body (when body (json/write-str body))})]
     (-> response
      :body
      (json/read-str :key-fn keyword)))))

(defn create-account! [context]
  (let [{:keys [account-number]} (http-request :post "account" {:name "Load test"})]
    [true (assoc-in context [:accounts account-number] 0)]))

(defn view-account [{:keys [:accounts]}]
  (let [{:keys [name]} (http-request :get (str "account/" (rand-nth (keys accounts))))]
    (= name "Load test")))

(defn post-deposit! [{:keys [:accounts] :as context}]
  (let [amount 100
        request-account-number (rand-nth (keys accounts))
        {:keys [account-number]} (http-request :post (str "account/" request-account-number "/deposit") {:amount amount})]
    [(= request-account-number account-number) (update-in context [:accounts request-account-number] + amount)]))

(defn withdraw! [{:keys [:accounts] :as context}]
  (let [amount 10
        request-account-number (rand-nth (keys accounts))
        {:keys [account-number]} (http-request :post (str "account/" request-account-number "/withdraw") {:amount amount})]
    [(= request-account-number account-number) (update-in context [:accounts request-account-number] - amount)]))

(defn transfer! [{:keys [accounts] :as context}]
  (let [amount 10
        credit-account-number (->> accounts (filter (fn [[_ v]] (> v 0))) ffirst)
        debit-account-number (rand-nth (keys accounts))
        {:keys [account-number]}
        (http-request :post (str "account/" credit-account-number "/send") {:amount amount
                                                                            :account-number debit-account-number})]
    [(= credit-account-number account-number) (-> context
                                                  (update-in [:accounts credit-account-number] - amount)
                                                  (update-in [:accounts debit-account-number] + amount))]))

(comment
  (create-account! {})
  (view-account {:accounts {1 0}})
  (post-deposit! {:accounts {1 0}})
  (withdraw! {:accounts {1 10}})
  (transfer! {:accounts {1 10
                         2 0}})
  (clj-gatling/run
   {:name "Load test"
    :scenarios [{:name "Create and view account"
                 :context {:accounts {}}
                 :steps [{:name "Create account"
                          :request create-account!}
                         {:name "View account"
                          :request view-account}]}
                {:name "Create account, post deposit, and withdraw"
                 :context {:accounts {}}
                 :steps [{:name "Create account"
                          :request create-account!}
                         {:name "Post deposit"
                          :request post-deposit!}
                         {:name "Withdraw"
                          :request withdraw!}]}
                {:name "Create two accounts, post deposit, and transfer"
                 :context {:accounts {}}
                 :steps [{:name "Create first account"
                          :request create-account!}
                         {:name "Create second account"
                          :request create-account!}
                         {:name "Post deposit"
                          :request post-deposit!}
                         {:name "Transfer"
                          :request transfer!}]}]}
   {:concurrency 3
    :requests 1000}))
