-- V5: remove the QA/test accounts seeded by V2/V3 and neutralise the public
-- bcrypt hash they shared.
--
-- * Deletes the Harbor Bowl Kitchen test restaurant (manager@harborbowl.test),
--   every *@test.com user and their drivers / vehicles / orders / deliveries.
-- * Any remaining account still using the public seed hash (e.g. the seeded
--   ADMIN robustcode@outlook.com) gets an unusable password. Set
--   ADMIN_BOOTSTRAP_EMAIL / ADMIN_BOOTSTRAP_PASSWORD so AdminBootstrap sets a
--   real password at startup (it runs after Flyway).
--
-- Idempotent: every statement is keyed on the test identifiers and becomes a
-- no-op once the data is gone. Real (non-test) rows are never deleted: if a
-- real order/delivery points at a test driver, only the driver link is cleared.

DO $$
DECLARE
    v_public_seed_hash CONSTANT TEXT := '$2y$10$ERR7izVduprY5SkhjUC5SO/fofoKmumIyhPys.fzAScg12zqEvJtW';
    v_test_user_ids BIGINT[];
    v_test_restaurant_ids BIGINT[];
    v_test_driver_ids BIGINT[];
    v_test_vehicle_ids BIGINT[];
BEGIN
    SELECT COALESCE(array_agg(id), ARRAY[]::BIGINT[])
    INTO v_test_user_ids
    FROM users
    WHERE email = 'manager@harborbowl.test'
       OR email ILIKE '%@test.com';

    SELECT COALESCE(array_agg(id), ARRAY[]::BIGINT[])
    INTO v_test_restaurant_ids
    FROM restaurants
    WHERE email = 'manager@harborbowl.test';

    SELECT COALESCE(array_agg(id), ARRAY[]::BIGINT[])
    INTO v_test_driver_ids
    FROM drivers
    WHERE user_id = ANY (v_test_user_ids);

    SELECT COALESCE(array_agg(DISTINCT vehicle_id), ARRAY[]::BIGINT[])
    INTO v_test_vehicle_ids
    FROM (
        SELECT vehicle_id FROM drivers WHERE id = ANY (v_test_driver_ids) AND vehicle_id IS NOT NULL
        UNION
        SELECT id FROM vehicles WHERE plate_number IN ('DRV-100-QA', 'DRV-200-QA', 'DRV-300-QA')
    ) AS candidate_vehicles;

    -- Orders / deliveries that belong to test customers or the test restaurant.
    DELETE FROM order_status_history
    WHERE order_id IN (
        SELECT id FROM orders
        WHERE customer_id = ANY (v_test_user_ids)
           OR restaurant_id = ANY (v_test_restaurant_ids)
    );

    DELETE FROM orders
    WHERE customer_id = ANY (v_test_user_ids)
       OR restaurant_id = ANY (v_test_restaurant_ids);

    DELETE FROM deliveries
    WHERE customer_id = ANY (v_test_user_ids)
       OR restaurant_id = ANY (v_test_restaurant_ids);

    -- Real orders / deliveries assigned to a test driver: keep them, unassign.
    UPDATE orders
    SET driver_id = NULL,
        updated_at = CURRENT_TIMESTAMP
    WHERE driver_id = ANY (v_test_driver_ids);

    UPDATE deliveries
    SET driver_id = NULL,
        status = CASE WHEN status IN ('ASSIGNED', 'PICKED_UP', 'IN_TRANSIT') THEN 'PENDING' ELSE status END,
        updated_at = CURRENT_TIMESTAMP
    WHERE driver_id = ANY (v_test_driver_ids);

    -- Real users linked to the test restaurant: detach them.
    UPDATE users
    SET restaurant_id = NULL,
        updated_at = CURRENT_TIMESTAMP
    WHERE restaurant_id = ANY (v_test_restaurant_ids)
      AND NOT (id = ANY (v_test_user_ids));

    DELETE FROM drivers
    WHERE id = ANY (v_test_driver_ids);

    -- Only delete test vehicles no longer referenced by any driver.
    DELETE FROM vehicles
    WHERE id = ANY (v_test_vehicle_ids)
      AND NOT EXISTS (SELECT 1 FROM drivers d WHERE d.vehicle_id = vehicles.id);

    DELETE FROM users
    WHERE id = ANY (v_test_user_ids);

    DELETE FROM restaurants
    WHERE id = ANY (v_test_restaurant_ids);

    -- Neutralise the publicly known seed password for any account left over
    -- (notably the seeded ADMIN). '!' prefix is never a valid bcrypt hash, so
    -- login is impossible until AdminBootstrap (or an admin) resets it.
    UPDATE users
    SET password = '!disabled-public-seed-password',
        updated_at = CURRENT_TIMESTAMP
    WHERE password = v_public_seed_hash;
END $$;
