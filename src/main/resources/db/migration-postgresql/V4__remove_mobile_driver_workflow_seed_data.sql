DO $$
DECLARE
    qa_addresses TEXT[] := ARRAY[
        'QA MOBILE - 12 Rue des Fleurs, Paris',
        'QA MOBILE - 8 Avenue Victor Hugo, Paris',
        'QA MOBILE - 44 Boulevard Saint-Germain, Paris'
    ];
BEGIN
    DELETE FROM deliveries
    WHERE delivery_address = ANY (qa_addresses);

    DELETE FROM orders
    WHERE delivery_address = ANY (qa_addresses);

    DELETE FROM users
    WHERE email IN (
        'lea.customer@test.com',
        'sam.customer@test.com',
        'ines.customer@test.com'
    )
    AND role = 'CUSTOMER'
    AND NOT EXISTS (
        SELECT 1
        FROM orders
        WHERE orders.customer_id = users.id
    )
    AND NOT EXISTS (
        SELECT 1
        FROM deliveries
        WHERE deliveries.customer_id = users.id
    );

    UPDATE drivers
    SET availability_status = 'AVAILABLE',
        updated_at = CURRENT_TIMESTAMP
    WHERE user_id IN (
        SELECT id
        FROM users
        WHERE email IN ('driver@test.com', 'nina.driver@test.com')
    )
    AND availability_status = 'BUSY';
END $$;