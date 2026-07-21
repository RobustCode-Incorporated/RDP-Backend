CREATE TABLE orders (

    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    customer_id BIGINT NOT NULL,

    restaurant_id BIGINT NOT NULL,

    driver_id BIGINT NULL,

    status VARCHAR(30) NOT NULL,

    total_price DECIMAL(10,2) NOT NULL,

    delivery_address VARCHAR(255) NOT NULL,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_order_customer
        FOREIGN KEY (customer_id)
        REFERENCES users(id),

    CONSTRAINT fk_order_restaurant
        FOREIGN KEY (restaurant_id)
        REFERENCES restaurants(id),

    CONSTRAINT fk_order_driver
        FOREIGN KEY (driver_id)
        REFERENCES drivers(id)
);