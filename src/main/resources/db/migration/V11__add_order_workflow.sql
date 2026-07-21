CREATE TABLE order_status_history (

    id BIGINT AUTO_INCREMENT PRIMARY KEY,

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