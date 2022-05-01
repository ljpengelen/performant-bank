(ns bank.db
  (:require [hugsql.adapter.next-jdbc :as next-adapter]
            [hugsql.core :as hugsql]))

(hugsql/set-adapter! (next-adapter/hugsql-adapter-next-jdbc))

(hugsql/def-db-fns "bank/db.sql")
(declare create-account!)
