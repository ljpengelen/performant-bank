create table "transaction" (
    transaction_number integer primary key,
    source_account_number integer not null,
    target_account_number integer,
    credit integer,
    debit integer,
    foreign key(source_account_number) references account(account_number),
    foreign key(target_account_number) references account(account_number)
)
