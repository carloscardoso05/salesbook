alter table customers
    add column version timestamp with time zone not null default now();
