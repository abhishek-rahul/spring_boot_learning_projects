create table if not exists products (
  id              bigserial primary key,
  sku             varchar(64) not null unique,
  name            varchar(255) not null,
  price_paise     bigint not null check (price_paise >= 0),
  created_at      timestamptz not null default now()
);

create table if not exists inventory (
  product_id      bigint primary key references products(id) on delete cascade,
  available_qty   integer not null check (available_qty >= 0),
  updated_at      timestamptz not null default now()
);

create table if not exists orders (
  id              bigserial primary key,
  user_id         bigint not null,
  status          varchar(32) not null,
  total_paise     bigint not null check (total_paise >= 0),
  created_at      timestamptz not null default now()
);

create table if not exists order_items (
  id              bigserial primary key,
  order_id        bigint not null references orders(id) on delete cascade,
  product_id      bigint not null references products(id),
  qty             integer not null check (qty > 0),
  price_paise     bigint not null check (price_paise >= 0)
);

create index if not exists idx_orders_user_id on orders(user_id);
create index if not exists idx_order_items_order_id on order_items(order_id);
