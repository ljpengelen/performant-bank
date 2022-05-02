create table withdrawal (
    withdrawal_number integer primary key,
    account_number integer not null,
    amount integer not null,
    foreign key(account_number) references account(account_number)
)
