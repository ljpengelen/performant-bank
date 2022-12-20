(ns user
  (:require [bank.core :refer [system-config]]
            [bank.db :as db]
            [bank.handlers :as h]
            [clojure.core.async :refer [<!!]]
            [clojure.java.browse :refer [browse-url]]
            [config.core :refer [reload-env]]
            [integrant-repl-autoreload.core :refer [start-auto-reset
                                                    stop-auto-reset]]
            [integrant.repl :refer [go halt reset set-prep!]]
            [integrant.repl.state :refer [system]]
            [migratus.core :as migratus]))

(set-prep! (constantly system-config))

(comment
  (go)
  (reload-env)
  (reset)
  (halt)
  (start-auto-reset)
  (stop-auto-reset)
  (browse-url "http://localhost:3000/api-docs"))

(defn datasource []
  (:bank.core/datasource system))

(comment
  (def account-a (db/create-account! (datasource) {:name "Luc Engelen"}))
  (def account-b (db/create-account! (datasource) {:name "Frits van Egters"}))
  account-a
  account-b
  
  (def account-c (<!! (h/create-account-chan (datasource) "Jan Wolkers")))
  (def account-d (<!! (h/create-account-chan (datasource) "Herman Brusselmans")))
  account-c
  account-d
  
  (<!! (h/get-account-chan (datasource) (:account-number account-c)))
  (<!! (h/get-account-chan (datasource) (:account-number account-d)))
  (<!! (h/post-deposit-chan (datasource) (:account-number account-c) 100))
  (<!! (h/make-withdrawal-chan (datasource) (:account-number account-c) 100))
  (<!! (h/make-transfer-chan (datasource) (:account-number account-c) (:account-number account-d) 50))
  
  (db/persist-transaction! (datasource) {:credit-account-number 154260
                                         :debit-account-number 2
                                         :amount 500})
  (db/get-transactions (datasource) {:account-number 154260})
  (db/set-balance! (datasource) {:account-number 154260 :balance 123})
  (db/update-balance! (datasource) {:account-number 1 :amount -100})
  (db/get-account (datasource) {:account-number 1}))

(defn migratus-config [migration-dir]
  {:store :database
   :migration-dir migration-dir
   :db {:datasource (datasource)}})

(comment
  (migratus/migrate (migratus-config "migrations/sqlite"))
  (migratus/rollback (migratus-config "migrations/sqlite"))
  (migratus/create (migratus-config "migrations/sqlite") "debit-credit"))
