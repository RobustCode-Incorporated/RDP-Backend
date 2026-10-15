-- V6: make `deliveries` the single core entity of the platform.
--
-- Pilot flow: a RESTAURANT creates a delivery for a customer who ordered by
-- phone / WhatsApp / Instagram (no customer account), ADMIN/DISPATCHER assigns
-- a driver, the DRIVER picks up / starts / delivers or fails.
--
-- * deliveries.customer_id becomes optional (legacy link to a CUSTOMER user);
--   the customer is now described by customer_name / customer_phone.
-- * new business fields: coordinates, notes, order reference, amounts (EUR),
--   payment mode, failure / cancellation reasons, lifecycle timestamps and an
--   optimistic-locking version.
-- * statuses: CREATED, ASSIGNED, PICKED_UP, IN_TRANSIT, DELIVERED, FAILED,
--   CANCELLED. Legacy PENDING -> CREATED. Legacy failures were stored as
--   CANCELLED with "ECHEC: <reason>" appended to the description -> FAILED.
-- * delivery_status_history records every transition.
-- * restaurants get optional coordinates (default pickup point), drivers get
--   last_location_at.
--
-- The orders / order_status_history tables are intentionally left in place
-- (no longer used by the application).

-- ---------------------------------------------------------------- restaurants

ALTER TABLE restaurants ADD COLUMN IF NOT EXISTS latitude DECIMAL(10,7);
ALTER TABLE restaurants ADD COLUMN IF NOT EXISTS longitude DECIMAL(10,7);

-- ---------------------------------------------------------------- drivers

ALTER TABLE drivers ADD COLUMN IF NOT EXISTS last_location_at TIMESTAMP;

-- ---------------------------------------------------------------- deliveries

ALTER TABLE deliveries ALTER COLUMN customer_id DROP NOT NULL;

ALTER TABLE deliveries
    ADD COLUMN customer_name       VARCHAR(150),
    ADD COLUMN customer_phone      VARCHAR(30),
    ADD COLUMN delivery_latitude   DECIMAL(10,7),
    ADD COLUMN delivery_longitude  DECIMAL(10,7),
    ADD COLUMN pickup_latitude     DECIMAL(10,7),
    ADD COLUMN pickup_longitude    DECIMAL(10,7),
    ADD COLUMN notes               VARCHAR(500),
    ADD COLUMN order_reference     VARCHAR(100),
    ADD COLUMN order_amount        DECIMAL(10,2),
    ADD COLUMN payment_mode        VARCHAR(30),
    ADD COLUMN cash_to_collect     DECIMAL(10,2),
    ADD COLUMN delivery_fee        DECIMAL(10,2),
    ADD COLUMN failure_reason      VARCHAR(500),
    ADD COLUMN cancellation_reason VARCHAR(500),
    ADD COLUMN ready_at            TIMESTAMP,
    ADD COLUMN assigned_at         TIMESTAMP,
    ADD COLUMN picked_up_at        TIMESTAMP,
    ADD COLUMN delivered_at        TIMESTAMP,
    ADD COLUMN failed_at           TIMESTAMP,
    ADD COLUMN cancelled_at        TIMESTAMP,
    ADD COLUMN version             BIGINT NOT NULL DEFAULT 0;

-- Existing rows: customer name from the legacy CUSTOMER user.
UPDATE deliveries d
SET customer_name = NULLIF(TRIM(CONCAT(u.first_name, ' ', u.last_name)), '')
FROM users u
WHERE d.customer_id = u.id
  AND d.customer_name IS NULL;

UPDATE deliveries
SET customer_name = 'Unknown customer'
WHERE customer_name IS NULL;

ALTER TABLE deliveries ALTER COLUMN customer_name SET NOT NULL;

-- Existing rows predate payment handling: nothing to collect.
UPDATE deliveries SET payment_mode = 'PREPAID' WHERE payment_mode IS NULL;
ALTER TABLE deliveries ALTER COLUMN payment_mode SET NOT NULL;

-- Legacy `price` was the delivery price.
UPDATE deliveries SET delivery_fee = price WHERE delivery_fee IS NULL AND price IS NOT NULL;

-- Pickup coordinates default from the restaurant (if ever set).
UPDATE deliveries d
SET pickup_latitude = r.latitude,
    pickup_longitude = r.longitude
FROM restaurants r
WHERE d.restaurant_id = r.id
  AND d.pickup_latitude IS NULL
  AND r.latitude IS NOT NULL
  AND r.longitude IS NOT NULL;

-- Status mapping.
UPDATE deliveries SET status = 'CREATED' WHERE status = 'PENDING';

UPDATE deliveries
SET status = 'FAILED',
    failure_reason = COALESCE(
        NULLIF(TRIM(SUBSTRING(description FROM 'ECHEC:(.*)$')), ''),
        'Unknown (migrated)'),
    description = NULLIF(TRIM(REGEXP_REPLACE(description, '\s*\|?\s*ECHEC:.*$', '')), ''),
    failed_at = COALESCE(updated_at, created_at)
WHERE status = 'CANCELLED'
  AND description LIKE '%ECHEC:%';

UPDATE deliveries
SET cancelled_at = COALESCE(updated_at, created_at)
WHERE status = 'CANCELLED' AND cancelled_at IS NULL;

UPDATE deliveries
SET delivered_at = COALESCE(updated_at, created_at)
WHERE status = 'DELIVERED' AND delivered_at IS NULL;

-- Best effort: the real assignment time is unknown for legacy rows.
UPDATE deliveries
SET assigned_at = COALESCE(updated_at, created_at)
WHERE driver_id IS NOT NULL AND assigned_at IS NULL;

ALTER TABLE deliveries
    ADD CONSTRAINT chk_deliveries_status CHECK (status IN (
        'CREATED', 'ASSIGNED', 'PICKED_UP', 'IN_TRANSIT', 'DELIVERED', 'FAILED', 'CANCELLED'));

ALTER TABLE deliveries
    ADD CONSTRAINT chk_deliveries_payment_mode CHECK (payment_mode IN ('PREPAID', 'CASH_ON_DELIVERY'));

CREATE INDEX IF NOT EXISTS idx_deliveries_status ON deliveries (status);
CREATE INDEX IF NOT EXISTS idx_deliveries_restaurant_created ON deliveries (restaurant_id, created_at);
CREATE INDEX IF NOT EXISTS idx_deliveries_driver ON deliveries (driver_id);

-- ---------------------------------------------------------------- history

CREATE TABLE delivery_status_history (
    id          BIGSERIAL PRIMARY KEY,
    delivery_id BIGINT       NOT NULL,
    event       VARCHAR(30)  NOT NULL,
    old_status  VARCHAR(30),
    new_status  VARCHAR(30)  NOT NULL,
    changed_by  VARCHAR(255) NOT NULL,
    note        VARCHAR(500),
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_delivery_status_history_delivery
        FOREIGN KEY (delivery_id) REFERENCES deliveries (id) ON DELETE CASCADE
);

CREATE INDEX idx_delivery_status_history_delivery ON delivery_status_history (delivery_id, created_at);

-- One synthetic entry per existing delivery so every delivery has a history.
INSERT INTO delivery_status_history (delivery_id, event, old_status, new_status, changed_by, note, created_at)
SELECT id, 'MIGRATED', NULL, status, 'system:migration-v6', 'State imported from v1 data', COALESCE(updated_at, created_at)
FROM deliveries;
