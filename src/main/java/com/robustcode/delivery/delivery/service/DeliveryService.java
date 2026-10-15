package com.robustcode.delivery.delivery.service;

import com.robustcode.delivery.common.exception.BusinessRuleException;
import com.robustcode.delivery.common.exception.ConflictException;
import com.robustcode.delivery.common.exception.ForbiddenException;
import com.robustcode.delivery.common.exception.NotFoundException;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.robustcode.delivery.delivery.domain.Delivery;
import com.robustcode.delivery.delivery.dto.CreateDeliveryRequest;
import com.robustcode.delivery.delivery.repository.DeliveryRepository;
import com.robustcode.delivery.driver.domain.Driver;
import com.robustcode.delivery.driver.repository.DriverRepository;
import com.robustcode.delivery.restaurant.domain.Restaurant;
import com.robustcode.delivery.restaurant.repository.RestaurantRepository;
import com.robustcode.delivery.security.AccessGuard;
import com.robustcode.delivery.user.domain.User;
import com.robustcode.delivery.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DeliveryService {

    private static final Set<Delivery.Status> ACTIVE_DRIVER_STATUSES = Set.of(
            Delivery.Status.ASSIGNED,
            Delivery.Status.PICKED_UP,
            Delivery.Status.IN_TRANSIT
    );

    private final DeliveryRepository repository;
    private final UserRepository userRepository;
    private final DriverRepository driverRepository;
    private final RestaurantRepository restaurantRepository;
    private final AccessGuard accessGuard;

    /**
     * ADMIN/DISPATCHER see every delivery, RESTAURANT only its restaurant's,
     * DRIVER only the ones assigned to them.
     */
    @Transactional(readOnly = true)
    public List<Delivery> findAllVisibleTo(String email) {

        User user = accessGuard.requireUser(email);

        if (AccessGuard.isAdminOrDispatcher(user)) {
            return repository.findAll();
        }

        Long restaurantId = AccessGuard.ownRestaurantId(user);
        if (restaurantId != null) {
            return repository.findByRestaurantId(restaurantId);
        }

        Long driverId = AccessGuard.ownDriverId(user);
        if (driverId != null) {
            return repository.findByDriverId(driverId);
        }

        if (user.getRole() == User.Role.RESTAURANT || user.getRole() == User.Role.DRIVER) {
            return List.of();
        }

        throw new ForbiddenException("You do not have access to deliveries");
    }

    @Transactional(readOnly = true)
    public Delivery findByIdVisibleTo(Long id, String email) {

        Delivery delivery = findById(id);
        User user = accessGuard.requireUser(email);

        if (AccessGuard.isAdminOrDispatcher(user)) {
            return delivery;
        }

        Long restaurantId = AccessGuard.ownRestaurantId(user);
        if (restaurantId != null && restaurantId.equals(delivery.getRestaurant().getId())) {
            return delivery;
        }

        Long driverId = AccessGuard.ownDriverId(user);
        if (driverId != null && delivery.getDriver() != null && driverId.equals(delivery.getDriver().getId())) {
            return delivery;
        }

        throw new ForbiddenException("You do not have access to this delivery");
    }

    public List<Delivery> findMyDeliveries(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (user.getDriver() == null) {
            throw new BusinessRuleException("No driver profile assigned to user");
        }

        Driver driver = user.getDriver();
        List<Delivery> deliveries = repository.findByDriver(driver);

        syncDriverAvailability(driver, deliveries);

        return deliveries;
    }

    private void syncDriverAvailability(Driver driver, List<Delivery> deliveries) {

        Driver.AvailabilityStatus nextStatus = deliveries.stream()
                .anyMatch(delivery -> ACTIVE_DRIVER_STATUSES.contains(delivery.getStatus()))
                        ? Driver.AvailabilityStatus.BUSY
                        : Driver.AvailabilityStatus.AVAILABLE;

        if (driver.getAvailabilityStatus() == nextStatus) {
            return;
        }

        driver.setAvailabilityStatus(nextStatus);
        driverRepository.save(driver);
    }

    public Delivery findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Delivery not found"));
    }

    /**
     * ADMIN/DISPATCHER create deliveries for any restaurant; a RESTAURANT
     * user only for its own restaurant.
     */
    @Transactional
    public Delivery create(CreateDeliveryRequest request, String email) {

        User creator = accessGuard.requireUser(email);

        if (!AccessGuard.isAdminOrDispatcher(creator)) {
            Long ownRestaurantId = AccessGuard.ownRestaurantId(creator);
            if (ownRestaurantId == null || !ownRestaurantId.equals(request.getRestaurantId())) {
                throw new ForbiddenException("You can only create deliveries for your own restaurant");
            }
        }

        User customer = userRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new NotFoundException("Customer not found"));

        if (customer.getRole() != User.Role.CUSTOMER) {
            throw new BusinessRuleException("User is not a customer");
        }

        Restaurant restaurant = restaurantRepository.findById(request.getRestaurantId())
                .orElseThrow(() -> new NotFoundException("Restaurant not found"));

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
                .orElseThrow(() -> new NotFoundException("Delivery not found"));

        if (delivery.getDriver() != null) {
            throw new ConflictException("Delivery already assigned");
        }

        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new NotFoundException("Driver not found"));

        if (driver.getAvailabilityStatus() != Driver.AvailabilityStatus.AVAILABLE) {
            throw new ConflictException("Driver is not available");
        }

        delivery.setDriver(driver);
        delivery.setStatus(Delivery.Status.ASSIGNED);

        driver.setAvailabilityStatus(Driver.AvailabilityStatus.BUSY);

        return repository.save(delivery);
    }

    private Delivery findAssignedDeliveryForDriver(Long deliveryId, String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (user.getDriver() == null) {
            throw new BusinessRuleException("No driver profile assigned to user");
        }

        Delivery delivery = repository.findById(deliveryId)
                .orElseThrow(() -> new NotFoundException("Delivery not found"));

        if (delivery.getDriver() == null || !delivery.getDriver().getId().equals(user.getDriver().getId())) {
            throw new ForbiddenException("Driver not assigned to this delivery");
        }

        return delivery;
    }

    public Delivery pickupDelivery(Long deliveryId, String email) {

        Delivery delivery = findAssignedDeliveryForDriver(deliveryId, email);

        if (delivery.getStatus() != Delivery.Status.ASSIGNED) {
            throw new ConflictException(
                    "Delivery must be ASSIGNED before pickup. Current status: "
                            + delivery.getStatus());
        }

        delivery.setStatus(Delivery.Status.PICKED_UP);

        return repository.save(delivery);
    }

    public Delivery startDelivery(Long deliveryId, String email) {

        Delivery delivery = findAssignedDeliveryForDriver(deliveryId, email);

        if (delivery.getStatus() != Delivery.Status.PICKED_UP) {
            throw new ConflictException(
                    "Delivery must be PICKED_UP before starting transit. Current status: "
                            + delivery.getStatus());
        }

        delivery.setStatus(Delivery.Status.IN_TRANSIT);

        return repository.save(delivery);
    }

    public Delivery completeDelivery(Long deliveryId, String email) {

        Delivery delivery = findAssignedDeliveryForDriver(deliveryId, email);

        if (delivery.getStatus() != Delivery.Status.IN_TRANSIT) {
            throw new ConflictException(
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
            throw new ConflictException(
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