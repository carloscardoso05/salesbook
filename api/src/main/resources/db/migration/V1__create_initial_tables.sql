create table customers
(
    id      int primary key generated always as identity,
    name    text           not null check ( trim(name) <> '' ),
    balance numeric(12, 2) not null
);

create unique index customers_name_lower_idx on customers (lower(name));

create table payments
(
    id          int primary key generated always as identity,
    value       numeric(12, 2) not null check ( value > 0 ),
    customer_id int            not null references customers (id) on delete restrict,
    paid_at     timestamp      not null
);

create table products
(
    id    int primary key generated always as identity,
    name  text not null check ( trim(name) <> '' ),
    stock int  not null check ( stock >= 0 )
);

create unique index products_name_lower_idx on products (lower(name));

create table orders
(
    id          int primary key generated always as identity,
    customer_id int       not null references customers (id) on delete restrict,
    ordered_at  timestamp not null
);

create table order_items
(
    id         int primary key generated always as identity,
    order_id   int            not null references orders (id) on delete cascade,
    product_id int            not null references products (id) on delete restrict,
    price      numeric(12, 2) not null check ( price >= 0 )
);