package com.robustcode.delivery.driver.repository;


import com.robustcode.delivery.driver.domain.Driver;
import com.robustcode.delivery.user.domain.User;

import org.springframework.data.jpa.repository.JpaRepository;

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

}