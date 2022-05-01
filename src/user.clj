(ns user
  (:require [bank.core :refer [system-config]]
            [clojure.java.browse :refer [browse-url]]
            [integrant-repl-autoreload.core :as igr-auto]
            [integrant.repl :refer [go reset set-prep!]]
            [integrant.repl.state :refer [system]]
            [migratus.core :as migratus]))

(set-prep! (constantly system-config))

(comment
  (go)
  (reset)
  (igr-auto/start-auto-reset)
  (browse-url "http://localhost:3000/api-docs"))

(defn migratus-config []
  {:store :database
   :db {:datasource (:datasource system)}})

(comment
  (migratus/migrate (migratus-config))
  (migratus/rollback (migratus-config))
  (migratus/create (migratus-config) "test"))
