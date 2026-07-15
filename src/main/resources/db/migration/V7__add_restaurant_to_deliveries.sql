ALTER TABLE deliveries
ADD COLUMN restaurant_id BIGINT NOT NULL;

ALTER TABLE deliveries
ADD CONSTRAINT fk_delivery_restaurant
FOREIGN KEY (restaurant_id)
REFERENCES restaurants(id);