create table transfer (
    transfer_number integer primary key,
    source_account_number integer not null,
    target_account_number integer not null,
    amount integer not null,
    foreign key(source_account_number) references account(account_number),
    foreign key(target_account_number) references account(account_number)
)
