alter table products
    add column version timestamp with time zone not null default now();
