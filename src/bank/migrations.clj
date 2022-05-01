(ns bank.migrations
  (:require [bank.db :as db]
            [migratus.core :as migratus]))

(def config {:store :database
             :db {:datasource @db/datasource}})

(comment
  (migratus/migrate config)
  (migratus/rollback config)
  (migratus/create config "account"))
