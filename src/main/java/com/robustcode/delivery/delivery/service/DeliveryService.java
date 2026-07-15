package com.robustcode.delivery.delivery.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.robustcode.delivery.delivery.domain.Delivery;
import com.robustcode.delivery.delivery.dto.CreateDeliveryRequest;
import com.robustcode.delivery.delivery.repository.DeliveryRepository;
import com.robustcode.delivery.user.domain.User;
import com.robustcode.delivery.user.repository.UserRepository;
import com.robustcode.delivery.driver.domain.Driver;
import com.robustcode.delivery.driver.repository.DriverRepository;
import com.robustcode.delivery.restaurant.domain.Restaurant;
import com.robustcode.delivery.restaurant.repository.RestaurantRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DeliveryService {

    private final DeliveryRepository repository;
    private final UserRepository userRepository;
    private final DriverRepository driverRepository;
    private final RestaurantRepository restaurantRepository;

    public List<Delivery> findAll() {
        return repository.findAll();
    }

    public List<Delivery> findMyDeliveries(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getDriver() == null) {
            throw new RuntimeException("No driver profile assigned to user");
        }

        return repository.findByDriver(user.getDriver());
    }

    public Delivery findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Delivery not found"));
    }

    public Delivery create(CreateDeliveryRequest request) {

        User customer = userRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        if (customer.getRole() != User.Role.CUSTOMER) {
            throw new RuntimeException("User is not a customer");
        }

        Restaurant restaurant = restaurantRepository.findById(request.getRestaurantId())
                .orElseThrow(() -> new RuntimeException("Restaurant not found"));

        Delivery delivery = Delivery.builder()
                .customer(customer)
                .restaurant(restaurant)
                .pickupAddress(request.getPickupAddress())
                .deliveryAddress(request.getDeliveryAddress())
                .description(request.getDescription())
                .build();

        return repository.save(delivery);
    }

    public Delivery assignDriver(Long deliveryId, Long driverId) {

        Delivery delivery = repository.findById(deliveryId)
                .orElseThrow(() -> new RuntimeException("Delivery not found"));

        if (delivery.getDriver() != null) {
            throw new RuntimeException("Delivery already assigned");
        }

        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new RuntimeException("Driver not found"));

        if (driver.getAvailabilityStatus() != Driver.AvailabilityStatus.AVAILABLE) {
            throw new RuntimeException("Driver is not available");
        }

        delivery.setDriver(driver);
        delivery.setStatus(Delivery.Status.ASSIGNED);

        driver.setAvailabilityStatus(Driver.AvailabilityStatus.BUSY);

        return repository.save(delivery);
    }

    private Delivery findAssignedDeliveryForDriver(Long deliveryId, String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getDriver() == null) {
            throw new RuntimeException("No driver profile assigned to user");
        }

        Delivery delivery = repository.findById(deliveryId)
                .orElseThrow(() -> new RuntimeException("Delivery not found"));

        if (delivery.getDriver() == null || !delivery.getDriver().getId().equals(user.getDriver().getId())) {
            throw new RuntimeException("Driver not assigned to this delivery");
        }

        return delivery;
    }

    public Delivery pickupDelivery(Long deliveryId, String email) {

        Delivery delivery = findAssignedDeliveryForDriver(deliveryId, email);

        if (delivery.getStatus() != Delivery.Status.ASSIGNED) {
            throw new RuntimeException(
                    "Delivery must be ASSIGNED before pickup. Current status: "
                            + delivery.getStatus());
        }

        delivery.setStatus(Delivery.Status.PICKED_UP);

        return repository.save(delivery);
    }

    public Delivery startDelivery(Long deliveryId, String email) {

        Delivery delivery = findAssignedDeliveryForDriver(deliveryId, email);

        if (delivery.getStatus() != Delivery.Status.PICKED_UP) {
            throw new RuntimeException(
                    "Delivery must be PICKED_UP before starting transit. Current status: "
                            + delivery.getStatus());
        }

        delivery.setStatus(Delivery.Status.IN_TRANSIT);

        return repository.save(delivery);
    }

    public Delivery completeDelivery(Long deliveryId, String email) {

        Delivery delivery = findAssignedDeliveryForDriver(deliveryId, email);

        if (delivery.getStatus() != Delivery.Status.IN_TRANSIT) {
            throw new RuntimeException(
                    "Delivery must be IN_TRANSIT before completion. Current status: "
                            + delivery.getStatus());
        }

        delivery.setStatus(Delivery.Status.DELIVERED);

        if (delivery.getDriver() != null) {
            delivery.getDriver().setAvailabilityStatus(
                    Driver.AvailabilityStatus.AVAILABLE);
        }

        return repository.save(delivery);
    }

    public Delivery failDelivery(Long deliveryId, String email, String reason) {

        Delivery delivery = findAssignedDeliveryForDriver(deliveryId, email);

        if (delivery.getStatus() != Delivery.Status.ASSIGNED
                && delivery.getStatus() != Delivery.Status.PICKED_UP
                && delivery.getStatus() != Delivery.Status.IN_TRANSIT) {
            throw new RuntimeException(
                    "Delivery can only be marked as failed from ASSIGNED, PICKED_UP, or IN_TRANSIT. Current status: "
                            + delivery.getStatus());
        }

        delivery.setStatus(Delivery.Status.CANCELLED);

        String existingDescription = delivery.getDescription() == null ? "" : delivery.getDescription().trim();
        String failureReason = reason == null ? "" : reason.trim();
        String mergedDescription = existingDescription.isEmpty()
                ? "ECHEC: " + failureReason
                : existingDescription + " | ECHEC: " + failureReason;
        delivery.setDescription(mergedDescription);

        if (delivery.getDriver() != null) {
            delivery.getDriver().setAvailabilityStatus(Driver.AvailabilityStatus.AVAILABLE);
        }

        return repository.save(delivery);
    }
}