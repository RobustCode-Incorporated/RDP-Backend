CREATE TABLE drivers (

    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    user_id BIGINT NOT NULL UNIQUE,

    phone_number VARCHAR(30) NOT NULL,

    vehicle_type VARCHAR(50),

    vehicle_plate VARCHAR(30),

    availability_status VARCHAR(30) NOT NULL,

    current_latitude DECIMAL(10,7),

    current_longitude DECIMAL(10,7),

    odoo_employee_id BIGINT,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP NULL,

    CONSTRAINT fk_driver_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
);