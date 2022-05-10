drop table if exists "transaction"
--;;
create table "transaction" (
    transaction_number integer primary key,
    credit_account_number integer,
    debit_account_number integer,
    amount integer,
    foreign key(credit_account_number) references account(account_number),
    foreign key(debit_account_number) references account(account_number)
)
