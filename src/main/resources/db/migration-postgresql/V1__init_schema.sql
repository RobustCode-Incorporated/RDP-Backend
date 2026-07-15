CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    role VARCHAR(50) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL
);

CREATE TABLE IF NOT EXISTS vehicles (
    id BIGSERIAL PRIMARY KEY,
    vehicle_type VARCHAR(50) NOT NULL,
    plate_number VARCHAR(50) NOT NULL UNIQUE,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NULL
);

CREATE TABLE IF NOT EXISTS drivers (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    phone_number VARCHAR(30) NOT NULL,
    vehicle_type VARCHAR(50),
    vehicle_plate VARCHAR(30),
    availability_status VARCHAR(30) NOT NULL,
    current_latitude DECIMAL(10,7),
    current_longitude DECIMAL(10,7),
    odoo_employee_id BIGINT,
    vehicle_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    CONSTRAINT fk_driver_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_driver_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicles(id)
);

CREATE TABLE IF NOT EXISTS restaurants (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    description VARCHAR(500),
    phone_number VARCHAR(30),
    email VARCHAR(255),
    address VARCHAR(255) NOT NULL,
    city VARCHAR(100),
    country VARCHAR(100),
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL
);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'users'
          AND column_name = 'restaurant_id'
    ) THEN
        ALTER TABLE users ADD COLUMN restaurant_id BIGINT NULL;
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_users_restaurant'
    ) THEN
        ALTER TABLE users
            ADD CONSTRAINT fk_users_restaurant
            FOREIGN KEY (restaurant_id)
            REFERENCES restaurants(id);
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS deliveries (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    driver_id BIGINT,
    restaurant_id BIGINT NOT NULL,
    pickup_address VARCHAR(255) NOT NULL,
    delivery_address VARCHAR(255) NOT NULL,
    description VARCHAR(500),
    status VARCHAR(50) NOT NULL,
    price DECIMAL(10,2),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    CONSTRAINT fk_delivery_customer FOREIGN KEY (customer_id) REFERENCES users(id),
    CONSTRAINT fk_delivery_driver FOREIGN KEY (driver_id) REFERENCES drivers(id),
    CONSTRAINT fk_delivery_restaurant FOREIGN KEY (restaurant_id) REFERENCES restaurants(id)
);

CREATE TABLE IF NOT EXISTS orders (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    restaurant_id BIGINT NOT NULL,
    driver_id BIGINT,
    status VARCHAR(30) NOT NULL,
    total_price DECIMAL(10,2) NOT NULL,
    delivery_address VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_order_customer FOREIGN KEY (customer_id) REFERENCES users(id),
    CONSTRAINT fk_order_restaurant FOREIGN KEY (restaurant_id) REFERENCES restaurants(id),
    CONSTRAINT fk_order_driver FOREIGN KEY (driver_id) REFERENCES drivers(id)
);

CREATE TABLE IF NOT EXISTS order_status_history (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    old_status VARCHAR(50),
    new_status VARCHAR(50) NOT NULL,
    changed_by VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_order_status_history_order
        FOREIGN KEY (order_id)
        REFERENCES orders(id)
        ON DELETE CASCADE
);
