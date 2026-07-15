DO $$
DECLARE
    v_password_hash TEXT := '$2y$10$ERR7izVduprY5SkhjUC5SO/fofoKmumIyhPys.fzAScg12zqEvJtW';
    v_restaurant_id BIGINT;
    v_driver_id BIGINT;
    v_driver_user_id BIGINT;
    v_customer_a_id BIGINT;
    v_customer_b_id BIGINT;
    v_customer_c_id BIGINT;
    v_order_assigned_id BIGINT;
    v_order_pickup_id BIGINT;
    v_order_transit_id BIGINT;
BEGIN
    SELECT id INTO v_restaurant_id
    FROM restaurants
    WHERE email = 'manager@harborbowl.test';

    SELECT u.id, d.id
    INTO v_driver_user_id, v_driver_id
    FROM users u
    JOIN drivers d ON d.user_id = u.id
    WHERE u.email = 'driver@test.com';

    INSERT INTO users (
        email,
        password,
        first_name,
        last_name,
        role,
        enabled,
        restaurant_id,
        created_at,
        updated_at
    ) VALUES
        (
            'lea.customer@test.com',
            v_password_hash,
            'Lea',
            'Martin',
            'CUSTOMER',
            TRUE,
            NULL,
            CURRENT_TIMESTAMP,
            CURRENT_TIMESTAMP
        ),
        (
            'sam.customer@test.com',
            v_password_hash,
            'Sam',
            'Dupont',
            'CUSTOMER',
            TRUE,
            NULL,
            CURRENT_TIMESTAMP,
            CURRENT_TIMESTAMP
        ),
        (
            'ines.customer@test.com',
            v_password_hash,
            'Ines',
            'Diallo',
            'CUSTOMER',
            TRUE,
            NULL,
            CURRENT_TIMESTAMP,
            CURRENT_TIMESTAMP
        )
    ON CONFLICT (email) DO UPDATE
    SET password = EXCLUDED.password,
        first_name = EXCLUDED.first_name,
        last_name = EXCLUDED.last_name,
        role = EXCLUDED.role,
        enabled = EXCLUDED.enabled,
        restaurant_id = NULL,
        updated_at = CURRENT_TIMESTAMP;

    SELECT id INTO v_customer_a_id FROM users WHERE email = 'lea.customer@test.com';
    SELECT id INTO v_customer_b_id FROM users WHERE email = 'sam.customer@test.com';
    SELECT id INTO v_customer_c_id FROM users WHERE email = 'ines.customer@test.com';

    INSERT INTO orders (
        customer_id,
        restaurant_id,
        driver_id,
        status,
        total_price,
        delivery_address,
        created_at,
        updated_at
    )
    SELECT
        v_customer_a_id,
        v_restaurant_id,
        v_driver_id,
        'READY_FOR_PICKUP',
        28.50,
        'QA MOBILE - 12 Rue des Fleurs, Paris',
        CURRENT_TIMESTAMP - INTERVAL '45 minutes',
        CURRENT_TIMESTAMP - INTERVAL '20 minutes'
    WHERE NOT EXISTS (
        SELECT 1 FROM orders WHERE delivery_address = 'QA MOBILE - 12 Rue des Fleurs, Paris'
    );

    INSERT INTO orders (
        customer_id,
        restaurant_id,
        driver_id,
        status,
        total_price,
        delivery_address,
        created_at,
        updated_at
    )
    SELECT
        v_customer_b_id,
        v_restaurant_id,
        v_driver_id,
        'PICKED_UP',
        34.90,
        'QA MOBILE - 8 Avenue Victor Hugo, Paris',
        CURRENT_TIMESTAMP - INTERVAL '70 minutes',
        CURRENT_TIMESTAMP - INTERVAL '12 minutes'
    WHERE NOT EXISTS (
        SELECT 1 FROM orders WHERE delivery_address = 'QA MOBILE - 8 Avenue Victor Hugo, Paris'
    );

    INSERT INTO orders (
        customer_id,
        restaurant_id,
        driver_id,
        status,
        total_price,
        delivery_address,
        created_at,
        updated_at
    )
    SELECT
        v_customer_c_id,
        v_restaurant_id,
        v_driver_id,
        'PICKED_UP',
        19.40,
        'QA MOBILE - 44 Boulevard Saint-Germain, Paris',
        CURRENT_TIMESTAMP - INTERVAL '95 minutes',
        CURRENT_TIMESTAMP - INTERVAL '5 minutes'
    WHERE NOT EXISTS (
        SELECT 1 FROM orders WHERE delivery_address = 'QA MOBILE - 44 Boulevard Saint-Germain, Paris'
    );

    SELECT id INTO v_order_assigned_id
    FROM orders
    WHERE delivery_address = 'QA MOBILE - 12 Rue des Fleurs, Paris';

    SELECT id INTO v_order_pickup_id
    FROM orders
    WHERE delivery_address = 'QA MOBILE - 8 Avenue Victor Hugo, Paris';

    SELECT id INTO v_order_transit_id
    FROM orders
    WHERE delivery_address = 'QA MOBILE - 44 Boulevard Saint-Germain, Paris';

    INSERT INTO order_status_history (order_id, old_status, new_status, changed_by, created_at)
    SELECT v_order_assigned_id, 'PENDING', 'ACCEPTED', 'qa-seed', CURRENT_TIMESTAMP - INTERVAL '40 minutes'
    WHERE NOT EXISTS (
        SELECT 1 FROM order_status_history WHERE order_id = v_order_assigned_id AND new_status = 'ACCEPTED'
    );

    INSERT INTO order_status_history (order_id, old_status, new_status, changed_by, created_at)
    SELECT v_order_assigned_id, 'ACCEPTED', 'READY_FOR_PICKUP', 'qa-seed', CURRENT_TIMESTAMP - INTERVAL '22 minutes'
    WHERE NOT EXISTS (
        SELECT 1 FROM order_status_history WHERE order_id = v_order_assigned_id AND new_status = 'READY_FOR_PICKUP'
    );

    INSERT INTO order_status_history (order_id, old_status, new_status, changed_by, created_at)
    SELECT v_order_pickup_id, 'PENDING', 'ACCEPTED', 'qa-seed', CURRENT_TIMESTAMP - INTERVAL '60 minutes'
    WHERE NOT EXISTS (
        SELECT 1 FROM order_status_history WHERE order_id = v_order_pickup_id AND new_status = 'ACCEPTED'
    );

    INSERT INTO order_status_history (order_id, old_status, new_status, changed_by, created_at)
    SELECT v_order_pickup_id, 'ACCEPTED', 'READY_FOR_PICKUP', 'qa-seed', CURRENT_TIMESTAMP - INTERVAL '35 minutes'
    WHERE NOT EXISTS (
        SELECT 1 FROM order_status_history WHERE order_id = v_order_pickup_id AND new_status = 'READY_FOR_PICKUP'
    );

    INSERT INTO order_status_history (order_id, old_status, new_status, changed_by, created_at)
    SELECT v_order_pickup_id, 'READY_FOR_PICKUP', 'PICKED_UP', 'qa-seed', CURRENT_TIMESTAMP - INTERVAL '12 minutes'
    WHERE NOT EXISTS (
        SELECT 1 FROM order_status_history WHERE order_id = v_order_pickup_id AND new_status = 'PICKED_UP'
    );

    INSERT INTO order_status_history (order_id, old_status, new_status, changed_by, created_at)
    SELECT v_order_transit_id, 'PENDING', 'ACCEPTED', 'qa-seed', CURRENT_TIMESTAMP - INTERVAL '85 minutes'
    WHERE NOT EXISTS (
        SELECT 1 FROM order_status_history WHERE order_id = v_order_transit_id AND new_status = 'ACCEPTED'
    );

    INSERT INTO order_status_history (order_id, old_status, new_status, changed_by, created_at)
    SELECT v_order_transit_id, 'ACCEPTED', 'READY_FOR_PICKUP', 'qa-seed', CURRENT_TIMESTAMP - INTERVAL '50 minutes'
    WHERE NOT EXISTS (
        SELECT 1 FROM order_status_history WHERE order_id = v_order_transit_id AND new_status = 'READY_FOR_PICKUP'
    );

    INSERT INTO order_status_history (order_id, old_status, new_status, changed_by, created_at)
    SELECT v_order_transit_id, 'READY_FOR_PICKUP', 'PICKED_UP', 'qa-seed', CURRENT_TIMESTAMP - INTERVAL '18 minutes'
    WHERE NOT EXISTS (
        SELECT 1 FROM order_status_history WHERE order_id = v_order_transit_id AND new_status = 'PICKED_UP'
    );

    INSERT INTO deliveries (
        customer_id,
        driver_id,
        restaurant_id,
        pickup_address,
        delivery_address,
        description,
        status,
        price,
        created_at,
        updated_at
    )
    SELECT
        v_customer_a_id,
        v_driver_id,
        v_restaurant_id,
        '18 Quai des Saveurs, Paris',
        'QA MOBILE - 12 Rue des Fleurs, Paris',
        'QA mobile assigned workflow - freshly prepared bowl menu.',
        'ASSIGNED',
        28.50,
        CURRENT_TIMESTAMP - INTERVAL '45 minutes',
        CURRENT_TIMESTAMP - INTERVAL '20 minutes'
    WHERE NOT EXISTS (
        SELECT 1 FROM deliveries WHERE delivery_address = 'QA MOBILE - 12 Rue des Fleurs, Paris'
    );

    INSERT INTO deliveries (
        customer_id,
        driver_id,
        restaurant_id,
        pickup_address,
        delivery_address,
        description,
        status,
        price,
        created_at,
        updated_at
    )
    SELECT
        v_customer_b_id,
        v_driver_id,
        v_restaurant_id,
        '18 Quai des Saveurs, Paris',
        'QA MOBILE - 8 Avenue Victor Hugo, Paris',
        'QA mobile pickup workflow - ready for start transition.',
        'PICKED_UP',
        34.90,
        CURRENT_TIMESTAMP - INTERVAL '70 minutes',
        CURRENT_TIMESTAMP - INTERVAL '12 minutes'
    WHERE NOT EXISTS (
        SELECT 1 FROM deliveries WHERE delivery_address = 'QA MOBILE - 8 Avenue Victor Hugo, Paris'
    );

    INSERT INTO deliveries (
        customer_id,
        driver_id,
        restaurant_id,
        pickup_address,
        delivery_address,
        description,
        status,
        price,
        created_at,
        updated_at
    )
    SELECT
        v_customer_c_id,
        v_driver_id,
        v_restaurant_id,
        '18 Quai des Saveurs, Paris',
        'QA MOBILE - 44 Boulevard Saint-Germain, Paris',
        'QA mobile transit workflow - should complete from the app.',
        'IN_TRANSIT',
        19.40,
        CURRENT_TIMESTAMP - INTERVAL '95 minutes',
        CURRENT_TIMESTAMP - INTERVAL '5 minutes'
    WHERE NOT EXISTS (
        SELECT 1 FROM deliveries WHERE delivery_address = 'QA MOBILE - 44 Boulevard Saint-Germain, Paris'
    );

    UPDATE drivers
    SET availability_status = 'BUSY',
        updated_at = CURRENT_TIMESTAMP
    WHERE id = v_driver_id;
END $$;