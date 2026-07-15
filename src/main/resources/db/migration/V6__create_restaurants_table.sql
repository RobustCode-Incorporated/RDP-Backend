CREATE TABLE restaurants (

    id BIGINT AUTO_INCREMENT PRIMARY KEY,

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