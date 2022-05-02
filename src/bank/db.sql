-- :name create-account! :<! :1
-- :doc Create an account and return it
insert into account (name, balance)
values (:name, 0)
returning account_number "account-number", name, balance

-- :name set-balance! :<! :1
-- :doc Set the balance of the account with the given number and return the account
update account
set balance = :balance
where account_number = :account-number
returning account_number "account-number", name, balance

-- :name get-account :? :1
-- :doc Get account by its number
select account_number "account-number", name, balance
from account where account_number = :account-number

-- :name persist-transaction! :n
-- :doc Persist a transaction
insert into "transaction" (source_account_number, target_account_number, credit, debit)
values (:source-account-number, :target-account-number, :credit, :debit)
