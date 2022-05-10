(ns user
  (:require [bank.core :refer [system-config]]
            [bank.db :as db]
            [clojure.java.browse :refer [browse-url]]
            [integrant-repl-autoreload.core :as igr-auto]
            [integrant.repl :refer [go halt reset set-prep!]]
            [integrant.repl.state :refer [system]]
            [migratus.core :as migratus]))

(set-prep! (constantly system-config))

(comment
  (go)
  (reset)
  (halt)
  (igr-auto/start-auto-reset)
  (browse-url "http://localhost:3000/api-docs"))

(defn datasource []
  (:bank.core/datasource system))

(comment
  (db/create-account! (datasource) {:name "Luc Engelen"})
  (db/persist-transaction! (datasource) {:credit-account-number 1
                                         :debit-account-number 2
                                         :amount 500})
  (db/get-transactions (datasource) {:account-number 2})
  (db/set-balance! (datasource) {:account-number 1 :balance 123}))

(defn migratus-config []
  {:store :database
   :db {:datasource (datasource)}})

(comment
  (migratus/migrate (migratus-config))
  (migratus/rollback (migratus-config))
  (migratus/create (migratus-config) "debit-credit"))
