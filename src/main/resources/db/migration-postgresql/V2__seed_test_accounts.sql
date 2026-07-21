DO $$
DECLARE
    v_password_hash TEXT := '$2y$10$ERR7izVduprY5SkhjUC5SO/fofoKmumIyhPys.fzAScg12zqEvJtW';
    v_restaurant_id BIGINT;
    v_driver_user_id BIGINT;
    v_driver_vehicle_id BIGINT;
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM restaurants
        WHERE email = 'manager@harborbowl.test'
    ) THEN
        INSERT INTO restaurants (
            name,
            description,
            phone_number,
            email,
            address,
            city,
            country,
            status,
            created_at,
            updated_at
        ) VALUES (
            'Harbor Bowl Kitchen',
            'Sample restaurant account for manual QA and workflow validation.',
            '+33 1 84 52 18 40',
            'manager@harborbowl.test',
            '18 Quai des Saveurs',
            'Paris',
            'France',
            'ACTIVE',
            CURRENT_TIMESTAMP,
            CURRENT_TIMESTAMP
        );
    END IF;

    SELECT id
    INTO v_restaurant_id
    FROM restaurants
    WHERE email = 'manager@harborbowl.test';

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
    ) VALUES (
        'robustcode@outlook.com',
        v_password_hash,
        'Platform',
        'Admin',
        'ADMIN',
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
    ) VALUES (
        'manager@harborbowl.test',
        v_password_hash,
        'Maya',
        'Fernand',
        'RESTAURANT',
        TRUE,
        v_restaurant_id,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    )
    ON CONFLICT (email) DO UPDATE
    SET password = EXCLUDED.password,
        first_name = EXCLUDED.first_name,
        last_name = EXCLUDED.last_name,
        role = EXCLUDED.role,
        enabled = EXCLUDED.enabled,
        restaurant_id = EXCLUDED.restaurant_id,
        updated_at = CURRENT_TIMESTAMP;

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
            'driver@test.com',
            v_password_hash,
            'Alex',
            'Rider',
            'DRIVER',
            TRUE,
            NULL,
            CURRENT_TIMESTAMP,
            CURRENT_TIMESTAMP
        ),
        (
            'nina.driver@test.com',
            v_password_hash,
            'Nina',
            'Lopez',
            'DRIVER',
            TRUE,
            NULL,
            CURRENT_TIMESTAMP,
            CURRENT_TIMESTAMP
        ),
        (
            'omar.driver@test.com',
            v_password_hash,
            'Omar',
            'Diallo',
            'DRIVER',
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

    IF NOT EXISTS (
        SELECT 1
        FROM vehicles
        WHERE plate_number = 'DRV-100-QA'
    ) THEN
        INSERT INTO vehicles (
            vehicle_type,
            plate_number,
            status,
            created_at,
            updated_at
        ) VALUES (
            'Scooter',
            'DRV-100-QA',
            'ASSIGNED',
            CURRENT_TIMESTAMP,
            CURRENT_TIMESTAMP
        );
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM vehicles
        WHERE plate_number = 'DRV-200-QA'
    ) THEN
        INSERT INTO vehicles (
            vehicle_type,
            plate_number,
            status,
            created_at,
            updated_at
        ) VALUES (
            'Bike',
            'DRV-200-QA',
            'ASSIGNED',
            CURRENT_TIMESTAMP,
            CURRENT_TIMESTAMP
        );
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM vehicles
        WHERE plate_number = 'DRV-300-QA'
    ) THEN
        INSERT INTO vehicles (
            vehicle_type,
            plate_number,
            status,
            created_at,
            updated_at
        ) VALUES (
            'Car',
            'DRV-300-QA',
            'ASSIGNED',
            CURRENT_TIMESTAMP,
            CURRENT_TIMESTAMP
        );
    END IF;

    FOR v_driver_user_id, v_driver_vehicle_id IN
        SELECT u.id, v.id
        FROM (
            VALUES
                ('driver@test.com', '+33 6 11 22 33 44', 'Scooter', 'DRV-100-QA', 'AVAILABLE'),
                ('nina.driver@test.com', '+33 6 22 33 44 55', 'Bike', 'DRV-200-QA', 'AVAILABLE'),
                ('omar.driver@test.com', '+33 6 33 44 55 66', 'Car', 'DRV-300-QA', 'OFFLINE')
        ) AS seed(email, phone_number, vehicle_type, plate_number, availability_status)
        JOIN users u ON u.email = seed.email
        JOIN vehicles v ON v.plate_number = seed.plate_number
    LOOP
        IF EXISTS (
            SELECT 1
            FROM drivers
            WHERE user_id = v_driver_user_id
        ) THEN
            UPDATE drivers
            SET vehicle_id = v_driver_vehicle_id,
                phone_number = (
                    SELECT seed.phone_number
                    FROM (
                        VALUES
                            ('driver@test.com', '+33 6 11 22 33 44', 'Scooter', 'DRV-100-QA', 'AVAILABLE'),
                            ('nina.driver@test.com', '+33 6 22 33 44 55', 'Bike', 'DRV-200-QA', 'AVAILABLE'),
                            ('omar.driver@test.com', '+33 6 33 44 55 66', 'Car', 'DRV-300-QA', 'OFFLINE')
                    ) AS seed(email, phone_number, vehicle_type, plate_number, availability_status)
                    JOIN users u2 ON u2.email = seed.email
                    WHERE u2.id = v_driver_user_id
                ),
                vehicle_type = (
                    SELECT seed.vehicle_type
                    FROM (
                        VALUES
                            ('driver@test.com', '+33 6 11 22 33 44', 'Scooter', 'DRV-100-QA', 'AVAILABLE'),
                            ('nina.driver@test.com', '+33 6 22 33 44 55', 'Bike', 'DRV-200-QA', 'AVAILABLE'),
                            ('omar.driver@test.com', '+33 6 33 44 55 66', 'Car', 'DRV-300-QA', 'OFFLINE')
                    ) AS seed(email, phone_number, vehicle_type, plate_number, availability_status)
                    JOIN users u2 ON u2.email = seed.email
                    WHERE u2.id = v_driver_user_id
                ),
                vehicle_plate = (
                    SELECT seed.plate_number
                    FROM (
                        VALUES
                            ('driver@test.com', '+33 6 11 22 33 44', 'Scooter', 'DRV-100-QA', 'AVAILABLE'),
                            ('nina.driver@test.com', '+33 6 22 33 44 55', 'Bike', 'DRV-200-QA', 'AVAILABLE'),
                            ('omar.driver@test.com', '+33 6 33 44 55 66', 'Car', 'DRV-300-QA', 'OFFLINE')
                    ) AS seed(email, phone_number, vehicle_type, plate_number, availability_status)
                    JOIN users u2 ON u2.email = seed.email
                    WHERE u2.id = v_driver_user_id
                ),
                availability_status = (
                    SELECT seed.availability_status
                    FROM (
                        VALUES
                            ('driver@test.com', '+33 6 11 22 33 44', 'Scooter', 'DRV-100-QA', 'AVAILABLE'),
                            ('nina.driver@test.com', '+33 6 22 33 44 55', 'Bike', 'DRV-200-QA', 'AVAILABLE'),
                            ('omar.driver@test.com', '+33 6 33 44 55 66', 'Car', 'DRV-300-QA', 'OFFLINE')
                    ) AS seed(email, phone_number, vehicle_type, plate_number, availability_status)
                    JOIN users u2 ON u2.email = seed.email
                    WHERE u2.id = v_driver_user_id
                ),
                updated_at = CURRENT_TIMESTAMP
            WHERE user_id = v_driver_user_id;
        ELSE
            INSERT INTO drivers (
                user_id,
                vehicle_id,
                phone_number,
                vehicle_type,
                vehicle_plate,
                availability_status,
                created_at,
                updated_at
            )
            SELECT
                u.id,
                v.id,
                seed.phone_number,
                seed.vehicle_type,
                seed.plate_number,
                seed.availability_status,
                CURRENT_TIMESTAMP,
                CURRENT_TIMESTAMP
            FROM (
                VALUES
                    ('driver@test.com', '+33 6 11 22 33 44', 'Scooter', 'DRV-100-QA', 'AVAILABLE'),
                    ('nina.driver@test.com', '+33 6 22 33 44 55', 'Bike', 'DRV-200-QA', 'AVAILABLE'),
                    ('omar.driver@test.com', '+33 6 33 44 55 66', 'Car', 'DRV-300-QA', 'OFFLINE')
            ) AS seed(email, phone_number, vehicle_type, plate_number, availability_status)
            JOIN users u ON u.email = seed.email
            JOIN vehicles v ON v.plate_number = seed.plate_number
            WHERE u.id = v_driver_user_id;
        END IF;
    END LOOP;
END $$;