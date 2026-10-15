package com.robustcode.delivery.delivery.service;

import java.time.LocalDateTime;
import java.util.Collection;

import org.springframework.data.jpa.domain.Specification;

import com.robustcode.delivery.delivery.domain.Delivery;

final class DeliverySpecifications {

    private DeliverySpecifications() {
    }

    static Specification<Delivery> restaurantId(Long restaurantId) {
        return (root, query, cb) -> cb.equal(root.get("restaurant").get("id"), restaurantId);
    }

    static Specification<Delivery> driverId(Long driverId) {
        return (root, query, cb) -> cb.equal(root.get("driver").get("id"), driverId);
    }

    static Specification<Delivery> statusIn(Collection<Delivery.Status> statuses) {
        return (root, query, cb) -> root.get("status").in(statuses);
    }

    static Specification<Delivery> createdAtFrom(LocalDateTime inclusive) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), inclusive);
    }

    static Specification<Delivery> createdAtBefore(LocalDateTime exclusive) {
        return (root, query, cb) -> cb.lessThan(root.get("createdAt"), exclusive);
    }
}
