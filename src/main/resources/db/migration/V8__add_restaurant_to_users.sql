ALTER TABLE users
ADD COLUMN restaurant_id BIGINT NULL;


ALTER TABLE users
ADD CONSTRAINT fk_users_restaurant
FOREIGN KEY (restaurant_id)
REFERENCES restaurants(id);