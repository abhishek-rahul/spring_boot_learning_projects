insert into products (sku, name, price_paise)
values
  ('SKU-APPLE-01', 'Apple', 1500),
  ('SKU-MILK-01',  'Milk',  5500),
  ('SKU-BREAD-01', 'Bread', 2500)
on conflict (sku) do update
set name = excluded.name,
    price_paise = excluded.price_paise;

insert into inventory (product_id, available_qty)
select p.id, v.qty
from (
  values
    ('SKU-APPLE-01', 100),
    ('SKU-MILK-01',  50),
    ('SKU-BREAD-01', 80)
) as v(sku, qty)
join products p on p.sku = v.sku
on conflict (product_id) do update
set available_qty = excluded.available_qty,
    updated_at = now();
