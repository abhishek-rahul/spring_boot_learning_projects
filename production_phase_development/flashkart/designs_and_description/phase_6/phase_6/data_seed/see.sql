BEGIN;

-- ============================================================================
-- Cleanup: Delete existing cart data (in correct order due to FK constraints)
-- ============================================================================
DELETE FROM cart_items;
DELETE FROM carts;

-- ============================================================================
-- Step 1: Select existing users (pick 3 users for seeding)
-- ============================================================================
WITH selected_users AS (
    SELECT id, email
    FROM users
    ORDER BY created_at
    LIMIT 3
)
SELECT id, email INTO TEMP TABLE temp_users FROM selected_users;

-- ============================================================================
-- Step 2: Select existing SKUs (pick enough for cart items)
-- ============================================================================
WITH selected_skus AS (
    SELECT id, sku_code, product_id
    FROM skus
    WHERE deleted_at IS NULL AND active = true
    ORDER BY created_at
    LIMIT 20
)
SELECT id, sku_code, product_id INTO TEMP TABLE temp_skus FROM selected_skus;

-- ============================================================================
-- Step 3: Insert active carts for 3 users
-- ============================================================================
-- User 1: Active cart (expires in 30 days)
INSERT INTO carts (
    id,
    user_id,
    expires_at,
    created_at,
    updated_at,
    deleted_at
)
SELECT
    gen_random_uuid(),
    tu.id,
    NOW() + INTERVAL '30 days',
    NOW() - INTERVAL '5 days',
    NOW() - INTERVAL '2 days',
    NULL
FROM temp_users tu
ORDER BY tu.id
LIMIT 1;

-- User 2: Active cart (expires in 25 days)
INSERT INTO carts (
    id,
    user_id,
    expires_at,
    created_at,
    updated_at,
    deleted_at
)
SELECT
    gen_random_uuid(),
    tu.id,
    NOW() + INTERVAL '25 days',
    NOW() - INTERVAL '3 days',
    NOW() - INTERVAL '1 day',
    NULL
FROM temp_users tu
ORDER BY tu.id
LIMIT 1 OFFSET 1;

-- User 3: Active cart (expires in 20 days)
INSERT INTO carts (
    id,
    user_id,
    expires_at,
    created_at,
    updated_at,
    deleted_at
)
SELECT
    gen_random_uuid(),
    tu.id,
    NOW() + INTERVAL '20 days',
    NOW() - INTERVAL '7 days',
    NOW() - INTERVAL '6 hours',
    NULL
FROM temp_users tu
ORDER BY tu.id
LIMIT 1 OFFSET 2;

-- ============================================================================
-- Step 4: Insert soft-deleted cart for User 1 (demonstrates history)
-- ============================================================================
INSERT INTO carts (
    id,
    user_id,
    expires_at,
    created_at,
    updated_at,
    deleted_at
)
SELECT
    gen_random_uuid(),
    tu.id,
    NOW() - INTERVAL '10 days',
    NOW() - INTERVAL '45 days',
    NOW() - INTERVAL '10 days',
    NOW() - INTERVAL '10 days'
FROM temp_users tu
ORDER BY tu.id
LIMIT 1;

-- ============================================================================
-- Step 5: Insert cart items for User 1's active cart (4 items)
-- ============================================================================
WITH user1_active_cart AS (
    SELECT c.id AS cart_id
    FROM carts c
    INNER JOIN temp_users tu ON c.user_id = tu.id
    WHERE c.deleted_at IS NULL
    ORDER BY tu.id
    LIMIT 1
),
selected_skus_for_cart1 AS (
    SELECT id FROM temp_skus ORDER BY id LIMIT 4 OFFSET 3
)
INSERT INTO cart_items (
    id,
    cart_id,
    sku_id,
    quantity,
    created_at,
    updated_at,
    deleted_at
)
SELECT
    gen_random_uuid(),
    uac.cart_id,
    ts.id,
    CASE 
        WHEN ROW_NUMBER() OVER (ORDER BY ts.id) = 1 THEN 2
        WHEN ROW_NUMBER() OVER (ORDER BY ts.id) = 2 THEN 1
        WHEN ROW_NUMBER() OVER (ORDER BY ts.id) = 3 THEN 3
        ELSE 1
    END,
    NOW() - INTERVAL '4 days',
    NOW() - INTERVAL '1 day',
    NULL
FROM selected_skus_for_cart1 ts
CROSS JOIN user1_active_cart uac
WHERE EXISTS (SELECT 1 FROM user1_active_cart);

-- ============================================================================
-- Step 6: Insert cart items for User 2's active cart (5 items)
-- ============================================================================
WITH user2_active_cart AS (
    SELECT c.id AS cart_id
    FROM carts c
    INNER JOIN temp_users tu ON c.user_id = tu.id
    WHERE c.deleted_at IS NULL
    ORDER BY tu.id
    LIMIT 1 OFFSET 1
),
selected_skus_for_cart2 AS (
    SELECT id FROM temp_skus ORDER BY id LIMIT 5 OFFSET 7
)
INSERT INTO cart_items (
    id,
    cart_id,
    sku_id,
    quantity,
    created_at,
    updated_at,
    deleted_at
)
SELECT
    gen_random_uuid(),
    uac.cart_id,
    ts.id,
    CASE 
        WHEN ROW_NUMBER() OVER (ORDER BY ts.id) = 1 THEN 1
        WHEN ROW_NUMBER() OVER (ORDER BY ts.id) = 2 THEN 2
        WHEN ROW_NUMBER() OVER (ORDER BY ts.id) = 3 THEN 1
        WHEN ROW_NUMBER() OVER (ORDER BY ts.id) = 4 THEN 4
        ELSE 2
    END,
    NOW() - INTERVAL '2 days',
    NOW() - INTERVAL '12 hours',
    NULL
FROM selected_skus_for_cart2 ts
CROSS JOIN user2_active_cart uac
WHERE EXISTS (SELECT 1 FROM user2_active_cart);

-- ============================================================================
-- Step 7: Insert cart items for User 3's active cart (6 items)
-- ============================================================================
WITH user3_active_cart AS (
    SELECT c.id AS cart_id
    FROM carts c
    INNER JOIN temp_users tu ON c.user_id = tu.id
    WHERE c.deleted_at IS NULL
    ORDER BY tu.id
    LIMIT 1 OFFSET 2
),
selected_skus_for_cart3 AS (
    SELECT id FROM temp_skus ORDER BY id LIMIT 6 OFFSET 12
)
INSERT INTO cart_items (
    id,
    cart_id,
    sku_id,
    quantity,
    created_at,
    updated_at,
    deleted_at
)
SELECT
    gen_random_uuid(),
    uac.cart_id,
    ts.id,
    CASE 
        WHEN ROW_NUMBER() OVER (ORDER BY ts.id) = 1 THEN 1
        WHEN ROW_NUMBER() OVER (ORDER BY ts.id) = 2 THEN 3
        WHEN ROW_NUMBER() OVER (ORDER BY ts.id) = 3 THEN 2
        WHEN ROW_NUMBER() OVER (ORDER BY ts.id) = 4 THEN 1
        WHEN ROW_NUMBER() OVER (ORDER BY ts.id) = 5 THEN 2
        ELSE 1
    END,
    NOW() - INTERVAL '6 days',
    NOW() - INTERVAL '5 hours',
    NULL
FROM selected_skus_for_cart3 ts
CROSS JOIN user3_active_cart uac
WHERE EXISTS (SELECT 1 FROM user3_active_cart);

-- ============================================================================
-- Step 8: Demonstrate soft-delete scenario for cart_item
-- ============================================================================
-- Use the FIRST SKU (offset 0) which was NOT used in Step 5
-- First insert a soft-deleted cart_item, then add a new active one
WITH user1_active_cart AS (
    SELECT c.id AS cart_id
    FROM carts c
    INNER JOIN temp_users tu ON c.user_id = tu.id
    WHERE c.deleted_at IS NULL
    ORDER BY tu.id
    LIMIT 1
),
demo_sku AS (
    SELECT id FROM temp_skus ORDER BY id LIMIT 1 OFFSET 0
)
INSERT INTO cart_items (
    id,
    cart_id,
    sku_id,
    quantity,
    created_at,
    updated_at,
    deleted_at
)
SELECT
    gen_random_uuid(),
    uac.cart_id,
    ds.id,
    5,
    NOW() - INTERVAL '10 days',
    NOW() - INTERVAL '8 days',
    NOW() - INTERVAL '8 days'
FROM user1_active_cart uac
CROSS JOIN demo_sku ds
WHERE EXISTS (SELECT 1 FROM user1_active_cart)
  AND EXISTS (SELECT 1 FROM demo_sku);

-- Now add the new active cart_item with same (cart_id, sku_id)
-- This is allowed because the old one is soft-deleted (deleted_at IS NOT NULL)
WITH user1_active_cart AS (
    SELECT c.id AS cart_id
    FROM carts c
    INNER JOIN temp_users tu ON c.user_id = tu.id
    WHERE c.deleted_at IS NULL
    ORDER BY tu.id
    LIMIT 1
),
demo_sku AS (
    SELECT id FROM temp_skus ORDER BY id LIMIT 1 OFFSET 0
)
INSERT INTO cart_items (
    id,
    cart_id,
    sku_id,
    quantity,
    created_at,
    updated_at,
    deleted_at
)
SELECT
    gen_random_uuid(),
    uac.cart_id,
    ds.id,
    2,
    NOW() - INTERVAL '2 days',
    NOW() - INTERVAL '1 day',
    NULL
FROM user1_active_cart uac
CROSS JOIN demo_sku ds
WHERE EXISTS (SELECT 1 FROM user1_active_cart)
  AND EXISTS (SELECT 1 FROM demo_sku);

-- ============================================================================
-- Cleanup temporary tables
-- ============================================================================
DROP TABLE IF EXISTS temp_users;
DROP TABLE IF EXISTS temp_skus;

COMMIT;