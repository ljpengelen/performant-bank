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

-- :name update-balance! :<! :1
-- :doc Add an amount to the balance of an account and return the account
update account
set balance = balance + :amount
where balance + :amount >= 0 and
    account_number = :account-number
returning account_number "account-number", name, balance

-- :name get-account :? :1
-- :doc Get account by its number
select account_number "account-number", name, balance
from account where account_number = :account-number

-- :name persist-transaction! :n
-- :doc Persist a transaction
insert into "transaction" (credit_account_number, debit_account_number, amount)
values (:credit-account-number, :debit-account-number, :amount)

-- :name get-transactions :? :*
-- :doc Get all transactions for an account
select * from "transaction"
where credit_account_number = :account-number or
    debit_account_number = :account-number
order by transaction_number desc
