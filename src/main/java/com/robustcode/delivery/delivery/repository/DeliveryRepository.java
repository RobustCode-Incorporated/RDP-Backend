package com.robustcode.delivery.delivery.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.robustcode.delivery.delivery.domain.Delivery;

/**
 * Restaurant, driver and the driver's user are fetched with the delivery so
 * DTO mapping works without open-in-view and without N+1 queries.
 */
public interface DeliveryRepository
        extends JpaRepository<Delivery, Long>, JpaSpecificationExecutor<Delivery> {

    @Override
    @EntityGraph(attributePaths = {"restaurant", "driver", "driver.user"})
    Optional<Delivery> findById(Long id);

    @Override
    @EntityGraph(attributePaths = {"restaurant", "driver", "driver.user"})
    List<Delivery> findAll(Specification<Delivery> spec, Sort sort);

    @EntityGraph(attributePaths = {"restaurant", "driver", "driver.user"})
    List<Delivery> findByDriverIdOrderByCreatedAtDesc(Long driverId);

    boolean existsByDriverIdAndStatusIn(Long driverId, java.util.Collection<Delivery.Status> statuses);
}
