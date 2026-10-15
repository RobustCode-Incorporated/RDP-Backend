-- Local MySQL equivalent of db/migration-postgresql/V6__unified_delivery_flow.sql.
-- See that file for the rationale. orders tables are left in place (unused).

ALTER TABLE restaurants
    ADD COLUMN latitude DECIMAL(10,7) NULL,
    ADD COLUMN longitude DECIMAL(10,7) NULL;

ALTER TABLE drivers
    ADD COLUMN last_location_at DATETIME(6) NULL;

ALTER TABLE deliveries MODIFY customer_id BIGINT NULL;

ALTER TABLE deliveries
    ADD COLUMN customer_name       VARCHAR(150) NULL,
    ADD COLUMN customer_phone      VARCHAR(30) NULL,
    ADD COLUMN delivery_latitude   DECIMAL(10,7) NULL,
    ADD COLUMN delivery_longitude  DECIMAL(10,7) NULL,
    ADD COLUMN pickup_latitude     DECIMAL(10,7) NULL,
    ADD COLUMN pickup_longitude    DECIMAL(10,7) NULL,
    ADD COLUMN notes               VARCHAR(500) NULL,
    ADD COLUMN order_reference     VARCHAR(100) NULL,
    ADD COLUMN order_amount        DECIMAL(10,2) NULL,
    ADD COLUMN payment_mode        VARCHAR(30) NULL,
    ADD COLUMN cash_to_collect     DECIMAL(10,2) NULL,
    ADD COLUMN delivery_fee        DECIMAL(10,2) NULL,
    ADD COLUMN failure_reason      VARCHAR(500) NULL,
    ADD COLUMN cancellation_reason VARCHAR(500) NULL,
    ADD COLUMN ready_at            DATETIME(6) NULL,
    ADD COLUMN assigned_at         DATETIME(6) NULL,
    ADD COLUMN picked_up_at        DATETIME(6) NULL,
    ADD COLUMN delivered_at        DATETIME(6) NULL,
    ADD COLUMN failed_at           DATETIME(6) NULL,
    ADD COLUMN cancelled_at        DATETIME(6) NULL,
    ADD COLUMN version             BIGINT NOT NULL DEFAULT 0;

UPDATE deliveries d
JOIN users u ON u.id = d.customer_id
SET d.customer_name = NULLIF(TRIM(CONCAT(u.first_name, ' ', u.last_name)), '')
WHERE d.customer_name IS NULL;

UPDATE deliveries SET customer_name = 'Unknown customer' WHERE customer_name IS NULL;

ALTER TABLE deliveries MODIFY customer_name VARCHAR(150) NOT NULL;

UPDATE deliveries SET payment_mode = 'PREPAID' WHERE payment_mode IS NULL;
ALTER TABLE deliveries MODIFY payment_mode VARCHAR(30) NOT NULL;

UPDATE deliveries SET delivery_fee = price WHERE delivery_fee IS NULL AND price IS NOT NULL;

UPDATE deliveries d
JOIN restaurants r ON r.id = d.restaurant_id
SET d.pickup_latitude = r.latitude,
    d.pickup_longitude = r.longitude
WHERE d.pickup_latitude IS NULL
  AND r.latitude IS NOT NULL
  AND r.longitude IS NOT NULL;

UPDATE deliveries SET status = 'CREATED' WHERE status = 'PENDING';

UPDATE deliveries
SET status = 'FAILED',
    failure_reason = COALESCE(
        NULLIF(TRIM(SUBSTRING(description, LOCATE('ECHEC:', description) + 6)), ''),
        'Unknown (migrated)'),
    description = NULLIF(TRIM(TRIM(TRAILING '|' FROM TRIM(SUBSTRING(description, 1, LOCATE('ECHEC:', description) - 1)))), ''),
    failed_at = COALESCE(updated_at, created_at)
WHERE status = 'CANCELLED'
  AND description LIKE '%ECHEC:%';

UPDATE deliveries
SET cancelled_at = COALESCE(updated_at, created_at)
WHERE status = 'CANCELLED' AND cancelled_at IS NULL;

UPDATE deliveries
SET delivered_at = COALESCE(updated_at, created_at)
WHERE status = 'DELIVERED' AND delivered_at IS NULL;

UPDATE deliveries
SET assigned_at = COALESCE(updated_at, created_at)
WHERE driver_id IS NOT NULL AND assigned_at IS NULL;

CREATE INDEX idx_deliveries_status ON deliveries (status);
CREATE INDEX idx_deliveries_restaurant_created ON deliveries (restaurant_id, created_at);

CREATE TABLE delivery_status_history (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    delivery_id BIGINT       NOT NULL,
    event       VARCHAR(30)  NOT NULL,
    old_status  VARCHAR(30)  NULL,
    new_status  VARCHAR(30)  NOT NULL,
    changed_by  VARCHAR(255) NOT NULL,
    note        VARCHAR(500) NULL,
    created_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_delivery_status_history_delivery
        FOREIGN KEY (delivery_id) REFERENCES deliveries (id) ON DELETE CASCADE,
    INDEX idx_delivery_status_history_delivery (delivery_id, created_at)
);

INSERT INTO delivery_status_history (delivery_id, event, old_status, new_status, changed_by, note, created_at)
SELECT id, 'MIGRATED', NULL, status, 'system:migration-v12', 'State imported from v1 data', COALESCE(updated_at, created_at)
FROM deliveries;
