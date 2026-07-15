CREATE TABLE vehicles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    vehicle_type VARCHAR(50) NOT NULL,

    plate_number VARCHAR(50) NOT NULL UNIQUE,

    status VARCHAR(50) NOT NULL,

    created_at DATETIME NOT NULL,

    updated_at DATETIME
);
