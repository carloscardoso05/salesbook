create table balance_adjustments
(
    id          int primary key generated always as identity,
    customer_id int            not null references customers (id) on delete restrict,
    value       numeric(12, 2) not null,
    adjusted_at timestamp      not null
);
