create or replace view v_order_summary as
select
  o.id as order_id,
  o.user_id,
  o.status,
  o.total_paise,
  o.created_at,
  count(oi.id) as item_count
from orders o
left join order_items oi on oi.order_id = o.id
group by o.id, o.user_id, o.status, o.total_paise, o.created_at;
