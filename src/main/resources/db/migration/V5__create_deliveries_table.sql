CREATE TABLE deliveries (

    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    customer_id BIGINT NOT NULL,

    driver_id BIGINT,

    pickup_address VARCHAR(255) NOT NULL,

    delivery_address VARCHAR(255) NOT NULL,

    description VARCHAR(500),

    status VARCHAR(50) NOT NULL,

    price DECIMAL(10,2),

    created_at DATETIME NOT NULL,

    updated_at DATETIME,

    CONSTRAINT fk_delivery_customer
        FOREIGN KEY (customer_id)
        REFERENCES users(id),

    CONSTRAINT fk_delivery_driver
        FOREIGN KEY (driver_id)
        REFERENCES drivers(id)

);