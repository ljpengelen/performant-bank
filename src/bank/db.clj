(ns bank.db
  (:require [hugsql.adapter.next-jdbc :as next-adapter]
            [hugsql.core :as hugsql]))

(hugsql/set-adapter! (next-adapter/hugsql-adapter-next-jdbc))

(defn def-db-fns []
  (binding [*ns* (find-ns 'bank.db)]
    (hugsql/def-db-fns "bank/db.sql")))

(declare create-account! get-account set-balance! persist-transaction!
         get-transactions update-balance!)
