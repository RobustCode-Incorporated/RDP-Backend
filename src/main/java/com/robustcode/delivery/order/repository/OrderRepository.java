package com.robustcode.delivery.order.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.robustcode.delivery.driver.domain.Driver;
import com.robustcode.delivery.order.domain.Order;
import com.robustcode.delivery.restaurant.domain.Restaurant;
import com.robustcode.delivery.user.domain.User;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByCustomer(User customer);

    List<Order> findByRestaurant(Restaurant restaurant);

    List<Order> findByDriver(Driver driver);

    List<Order> findByStatus(Order.Status status);

}