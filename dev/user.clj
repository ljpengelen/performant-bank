(ns user
  (:require [bank.core :refer [system-config]]
            [bank.core-async.domain :as async-domain]
            [bank.db :as db]
            [bank.domain :as domain]
            [clojure.core.async :refer [<!!]]
            [clojure.java.browse :refer [browse-url]]
            [config.core :refer [reload-env]]
            [integrant-repl-autoreload.core :refer [start-auto-reset
                                                    stop-auto-reset]]
            [integrant.repl :refer [go halt reset set-prep!]]
            [integrant.repl.state :refer [system]]
            [migratus.core :as migratus]))

(set-prep! (fn []
             (reload-env)
             (system-config)))

(comment
  (go)
  (reload-env)
  (reset)
  (halt)
  (start-auto-reset {:paths ["src" "resources" "dev"]})
  (stop-auto-reset)
  (browse-url "http://localhost:3000/api-docs"))

(defn datasource []
  (:bank.core/datasource system))

(comment
  (def bank async-domain/bank)
  
  (def account-a (db/create-account! (datasource) {:name "Luc Engelen"}))
  (def account-b (db/create-account! (datasource) {:name "Frits van Egters"}))
  account-a
  account-b
  
  (def account-c (<!! (domain/create-account! bank (datasource) "Jan Wolkers")))
  (def account-d (<!! (domain/create-account! bank (datasource) "Herman Brusselmans")))
  account-c
  account-d
  
  (<!! (domain/get-account bank (datasource) (:account-number account-c)))
  (<!! (domain/get-account bank (datasource) (:account-number account-d)))
  (<!! (domain/post-deposit! bank (datasource) (:account-number account-c) 100))
  (<!! (domain/make-withdrawal! bank (datasource) (:account-number account-c) 100))
  (<!! (domain/make-transfer! bank (datasource) (:account-number account-c) (:account-number account-d) 50))
  
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
