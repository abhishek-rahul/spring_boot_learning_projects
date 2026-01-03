create table if not exists payments (
  id              bigserial primary key,
  order_id        bigint not null unique references orders(id) on delete cascade,
  status          varchar(32) not null,
  provider        varchar(32) not null,
  created_at      timestamptz not null default now()
);

create index if not exists idx_payments_status on payments(status);
