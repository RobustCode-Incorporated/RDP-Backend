ALTER TABLE drivers
ADD COLUMN vehicle_id BIGINT;

ALTER TABLE drivers
ADD CONSTRAINT fk_driver_vehicle
FOREIGN KEY (vehicle_id)
REFERENCES vehicles(id);