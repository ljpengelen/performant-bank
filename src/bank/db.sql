-- :name create-account! :<!
-- :doc Create an account and return its number
insert into account (name, balance)
values (:name, 0)
returning account_number "account-number", name, balance
