package com.robustcode.delivery.driver.repository;


import com.robustcode.delivery.driver.domain.Driver;
import com.robustcode.delivery.user.domain.User;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;



public interface DriverRepository 
        extends JpaRepository<Driver, Long> {



    Optional<Driver> findByUser(User user);



    boolean existsByUser(User user);



    List<Driver> findByAvailabilityStatus(
            Driver.AvailabilityStatus availabilityStatus
    );



    List<Driver> findByAvailabilityStatusOrderByCreatedAtAsc(
            Driver.AvailabilityStatus availabilityStatus
    );



    /**
     * Locks the driver row for the rest of the transaction so two concurrent
     * assignments cannot both see the driver as AVAILABLE.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from Driver d join fetch d.user where d.id = :id")
    Optional<Driver> findByIdForUpdate(@Param("id") Long id);

    @Query("select d from Driver d join fetch d.user u"
            + " where d.availabilityStatus = :status and u.enabled = true"
            + " order by d.lastLocationAt desc nulls last, d.id asc")
    List<Driver> findEnabledByAvailabilityStatus(@Param("status") Driver.AvailabilityStatus status);

}
