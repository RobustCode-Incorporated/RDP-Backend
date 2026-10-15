package com.robustcode.delivery.delivery.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.robustcode.delivery.common.exception.BusinessRuleException;
import com.robustcode.delivery.common.exception.ConflictException;
import com.robustcode.delivery.common.exception.FieldValidationException;
import com.robustcode.delivery.common.exception.ForbiddenException;
import com.robustcode.delivery.common.exception.NotFoundException;
import com.robustcode.delivery.delivery.domain.Delivery;
import com.robustcode.delivery.delivery.domain.Delivery.Status;
import com.robustcode.delivery.delivery.domain.DeliveryStatusHistory;
import com.robustcode.delivery.delivery.domain.DeliveryStatusHistory.Event;
import com.robustcode.delivery.delivery.dto.CreateDeliveryRequest;
import com.robustcode.delivery.delivery.dto.DeliveryHistoryResponse;
import com.robustcode.delivery.delivery.dto.DeliveryResponse;
import com.robustcode.delivery.delivery.repository.DeliveryRepository;
import com.robustcode.delivery.delivery.repository.DeliveryStatusHistoryRepository;
import com.robustcode.delivery.driver.domain.Driver;
import com.robustcode.delivery.driver.repository.DriverRepository;
import com.robustcode.delivery.restaurant.domain.Restaurant;
import com.robustcode.delivery.restaurant.repository.RestaurantRepository;
import com.robustcode.delivery.security.AccessGuard;
import com.robustcode.delivery.user.domain.User;

import lombok.RequiredArgsConstructor;

/**
 * Single entry point for the delivery lifecycle.
 *
 * <pre>
 * CREATED ──assign──▶ ASSIGNED ──pickup──▶ PICKED_UP ──start──▶ IN_TRANSIT ──complete──▶ DELIVERED
 *    │                 │  ▲ reassign             │                    │
 *    │                 └──┘ (before pickup)      │                    │
 *    │ cancel          │ cancel / fail           │ fail               │ fail
 *    ▼                 ▼                         ▼                    ▼
 * CANCELLED     CANCELLED / FAILED            FAILED               FAILED
 * </pre>
 *
 * Every transition is validated by {@link Status#canTransitionTo(Status)},
 * written to {@code delivery_status_history} (changed_by = user email) and
 * keeps the driver availability (AVAILABLE / BUSY) consistent.
 */
@Service
@RequiredArgsConstructor
public class DeliveryService {

    /** Pilot restaurants are in Belgium: plain dates in filters use this zone. */
    static final ZoneId BUSINESS_ZONE = ZoneId.of("Europe/Brussels");

    private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));

    private final DeliveryRepository repository;
    private final DeliveryStatusHistoryRepository historyRepository;
    private final DriverRepository driverRepository;
    private final RestaurantRepository restaurantRepository;
    private final AccessGuard accessGuard;

    // ================================================================ queries

    /**
     * ADMIN/DISPATCHER: every delivery. RESTAURANT: its restaurant's.
     * DRIVER: the ones assigned to them. Newest first.
     */
    @Transactional(readOnly = true)
    public List<DeliveryResponse> search(String email, List<Status> statuses, String from, String to) {

        User user = accessGuard.requireUser(email);
        List<Specification<Delivery>> filters = new ArrayList<>();

        if (!AccessGuard.isAdminOrDispatcher(user)) {
            if (user.getRole() == User.Role.RESTAURANT) {
                Long restaurantId = AccessGuard.ownRestaurantId(user);
                if (restaurantId == null) {
                    return List.of();
                }
                filters.add(DeliverySpecifications.restaurantId(restaurantId));
            } else if (user.getRole() == User.Role.DRIVER) {
                Long driverId = AccessGuard.ownDriverId(user);
                if (driverId == null) {
                    return List.of();
                }
                filters.add(DeliverySpecifications.driverId(driverId));
            } else {
                throw new ForbiddenException("You do not have access to deliveries");
            }
        }

        addCommonFilters(filters, statuses, from, to);

        return repository.findAll(Specification.allOf(filters), NEWEST_FIRST)
                .stream()
                .map(DeliveryResponse::from)
                .toList();
    }

    /**
     * {@code /api/restaurants/{id}/deliveries}: ADMIN for any restaurant,
     * RESTAURANT only for its own.
     */
    @Transactional(readOnly = true)
    public List<DeliveryResponse> searchForRestaurant(Long restaurantId, List<Status> statuses, String email) {

        accessGuard.checkRestaurantAccess(accessGuard.requireUser(email), restaurantId);

        if (!restaurantRepository.existsById(restaurantId)) {
            throw new NotFoundException("Restaurant not found");
        }

        List<Specification<Delivery>> filters = new ArrayList<>();
        filters.add(DeliverySpecifications.restaurantId(restaurantId));
        addCommonFilters(filters, statuses, null, null);

        return repository.findAll(Specification.allOf(filters), NEWEST_FIRST)
                .stream()
                .map(DeliveryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public DeliveryResponse getById(Long id, String email) {
        return DeliveryResponse.from(loadVisible(id, accessGuard.requireUser(email)));
    }

    @Transactional(readOnly = true)
    public List<DeliveryHistoryResponse> getHistory(Long id, String email) {
        Delivery delivery = loadVisible(id, accessGuard.requireUser(email));
        return historyRepository.findByDeliveryIdOrderByCreatedAtAscIdAsc(delivery.getId())
                .stream()
                .map(DeliveryHistoryResponse::from)
                .toList();
    }

    /**
     * Mobile driver app: every delivery assigned to the calling driver.
     *
     * <p>Kept from v1 for mobile compatibility: calling this endpoint also
     * puts the driver online, i.e. sets AVAILABLE when they have no active
     * delivery and BUSY when they have one.</p>
     */
    @Transactional
    public List<DeliveryResponse> findMyDeliveries(String email) {

        User user = accessGuard.requireUser(email);
        Driver driver = requireDriverProfile(user);

        List<Delivery> deliveries = repository.findByDriverIdOrderByCreatedAtDesc(driver.getId());

        boolean busy = deliveries.stream().anyMatch(d -> Status.DRIVER_ACTIVE.contains(d.getStatus()));
        Driver.AvailabilityStatus expected = busy ? Driver.AvailabilityStatus.BUSY : Driver.AvailabilityStatus.AVAILABLE;
        if (driver.getAvailabilityStatus() != expected) {
            driver.setAvailabilityStatus(expected);
        }

        return deliveries.stream().map(DeliveryResponse::from).toList();
    }

    // ================================================================ restaurant / admin commands

    /**
     * RESTAURANT creates for its own restaurant (restaurantId in the body is
     * ignored); ADMIN/DISPATCHER must give restaurantId.
     */
    @Transactional
    public DeliveryResponse create(CreateDeliveryRequest request, String email) {

        User creator = accessGuard.requireUser(email);
        Restaurant restaurant = resolveRestaurantForCreation(creator, request.restaurantId());

        if (restaurant.getStatus() != Restaurant.Status.ACTIVE) {
            throw new BusinessRuleException("Restaurant is inactive");
        }

        validateCreation(request);

        boolean pickupCoordinatesGiven = request.pickupLatitude() != null;
        boolean cashOnDelivery = request.paymentMode() == Delivery.PaymentMode.CASH_ON_DELIVERY;
        String pickupAddress = trimToNull(request.pickupAddress());

        Delivery delivery = Delivery.builder()
                .restaurant(restaurant)
                .status(Status.CREATED)
                .customerName(request.customerName().trim())
                .customerPhone(request.customerPhone().trim())
                .deliveryAddress(request.deliveryAddress().trim())
                .deliveryLatitude(coordinate(request.deliveryLatitude()))
                .deliveryLongitude(coordinate(request.deliveryLongitude()))
                .pickupAddress(pickupAddress != null ? pickupAddress : restaurantAddress(restaurant))
                .pickupLatitude(pickupCoordinatesGiven ? coordinate(request.pickupLatitude()) : restaurant.getLatitude())
                .pickupLongitude(pickupCoordinatesGiven ? coordinate(request.pickupLongitude()) : restaurant.getLongitude())
                .description(trimToNull(request.description()))
                .notes(trimToNull(request.notes()))
                .orderReference(trimToNull(request.orderReference()))
                .orderAmount(money(request.orderAmount()))
                .paymentMode(request.paymentMode())
                .cashToCollect(cashOnDelivery ? money(request.cashToCollect()) : null)
                .deliveryFee(money(request.deliveryFee()))
                .build();

        delivery = repository.saveAndFlush(delivery);
        record(delivery, Event.CREATED, null, Status.CREATED, email, null);

        return DeliveryResponse.from(delivery);
    }

    /**
     * ADMIN/DISPATCHER: CREATED -> ASSIGNED, or ASSIGNED -> ASSIGNED
     * (reassignment before pickup; the previous driver becomes AVAILABLE).
     * Re-assigning the same driver is a no-op.
     */
    @Transactional
    public DeliveryResponse assignDriver(Long deliveryId, Long driverId, String email) {

        User actor = accessGuard.requireUser(email);
        if (!AccessGuard.isAdminOrDispatcher(actor)) {
            throw new ForbiddenException("Only an admin or dispatcher can assign drivers");
        }

        Delivery delivery = load(deliveryId);
        Status oldStatus = delivery.getStatus();
        Driver previousDriver = delivery.getDriver();

        if (oldStatus == Status.ASSIGNED && previousDriver != null && previousDriver.getId().equals(driverId)) {
            return DeliveryResponse.from(delivery);
        }

        if (!oldStatus.canTransitionTo(Status.ASSIGNED)) {
            throw new ConflictException(
                    "A driver can only be assigned while the delivery is CREATED or ASSIGNED (before pickup). Current status: "
                            + oldStatus);
        }

        Driver driver = driverRepository.findByIdForUpdate(driverId)
                .orElseThrow(() -> new NotFoundException("Driver not found"));

        if (!driver.getUser().isEnabled()) {
            throw new BusinessRuleException("Driver account is disabled");
        }
        if (driver.getAvailabilityStatus() != Driver.AvailabilityStatus.AVAILABLE) {
            throw new ConflictException("Driver is not available. Current availability: " + driver.getAvailabilityStatus());
        }

        delivery.setDriver(driver);
        delivery.setStatus(Status.ASSIGNED);
        delivery.setAssignedAt(LocalDateTime.now());
        driver.setAvailabilityStatus(Driver.AvailabilityStatus.BUSY);

        delivery = repository.saveAndFlush(delivery);

        if (previousDriver != null) {
            releaseDriver(previousDriver);
            record(delivery, Event.REASSIGNED, oldStatus, Status.ASSIGNED, email,
                    "Reassigned from driver #" + previousDriver.getId() + " to driver #" + driver.getId());
        } else {
            record(delivery, Event.ASSIGNED, oldStatus, Status.ASSIGNED, email,
                    "Assigned to driver #" + driver.getId());
        }

        return DeliveryResponse.from(delivery);
    }

    /**
     * RESTAURANT (own) or ADMIN: food is ready for pickup. Sets readyAt, no
     * status change. Idempotent.
     */
    @Transactional
    public DeliveryResponse markReady(Long deliveryId, String email) {

        User actor = accessGuard.requireUser(email);
        Delivery delivery = load(deliveryId);
        checkRestaurantOwnerOrAdmin(actor, delivery);

        if (delivery.getStatus() != Status.CREATED && delivery.getStatus() != Status.ASSIGNED) {
            throw new ConflictException(
                    "Food can only be marked ready before pickup. Current status: " + delivery.getStatus());
        }

        if (delivery.getReadyAt() != null) {
            return DeliveryResponse.from(delivery);
        }

        delivery.setReadyAt(LocalDateTime.now());
        delivery = repository.saveAndFlush(delivery);
        record(delivery, Event.READY, delivery.getStatus(), delivery.getStatus(), email, "Food ready for pickup");

        return DeliveryResponse.from(delivery);
    }

    /**
     * RESTAURANT (own) or ADMIN: CREATED|ASSIGNED -> CANCELLED. Frees the driver.
     */
    @Transactional
    public DeliveryResponse cancel(Long deliveryId, String reason, String email) {

        User actor = accessGuard.requireUser(email);
        Delivery delivery = load(deliveryId);
        checkRestaurantOwnerOrAdmin(actor, delivery);

        Status oldStatus = transition(delivery, Status.CANCELLED);
        String cleanReason = trimToNull(reason);
        delivery.setCancellationReason(cleanReason);
        delivery.setCancelledAt(LocalDateTime.now());

        delivery = repository.saveAndFlush(delivery);
        if (delivery.getDriver() != null) {
            releaseDriver(delivery.getDriver());
        }
        record(delivery, Event.CANCELLED, oldStatus, Status.CANCELLED, email, cleanReason);

        return DeliveryResponse.from(delivery);
    }

    // ================================================================ driver commands

    @Transactional
    public DeliveryResponse pickupDelivery(Long deliveryId, String email) {

        Delivery delivery = loadAssignedToDriver(deliveryId, email);
        Status oldStatus = transition(delivery, Status.PICKED_UP);
        delivery.setPickedUpAt(LocalDateTime.now());

        delivery = repository.saveAndFlush(delivery);
        record(delivery, Event.PICKED_UP, oldStatus, Status.PICKED_UP, email, null);

        return DeliveryResponse.from(delivery);
    }

    @Transactional
    public DeliveryResponse startDelivery(Long deliveryId, String email) {

        Delivery delivery = loadAssignedToDriver(deliveryId, email);
        Status oldStatus = transition(delivery, Status.IN_TRANSIT);

        delivery = repository.saveAndFlush(delivery);
        record(delivery, Event.IN_TRANSIT, oldStatus, Status.IN_TRANSIT, email, null);

        return DeliveryResponse.from(delivery);
    }

    @Transactional
    public DeliveryResponse completeDelivery(Long deliveryId, String email) {

        Delivery delivery = loadAssignedToDriver(deliveryId, email);
        Status oldStatus = transition(delivery, Status.DELIVERED);
        delivery.setDeliveredAt(LocalDateTime.now());

        delivery = repository.saveAndFlush(delivery);
        releaseDriver(delivery.getDriver());
        record(delivery, Event.DELIVERED, oldStatus, Status.DELIVERED, email, null);

        return DeliveryResponse.from(delivery);
    }

    @Transactional
    public DeliveryResponse failDelivery(Long deliveryId, String email, String reason) {

        String cleanReason = trimToNull(reason);
        if (cleanReason == null) {
            throw new FieldValidationException(Map.of("reason", "Failure reason is required"));
        }

        Delivery delivery = loadAssignedToDriver(deliveryId, email);
        Status oldStatus = transition(delivery, Status.FAILED);
        delivery.setFailureReason(cleanReason);
        delivery.setFailedAt(LocalDateTime.now());

        delivery = repository.saveAndFlush(delivery);
        releaseDriver(delivery.getDriver());
        record(delivery, Event.FAILED, oldStatus, Status.FAILED, email, cleanReason);

        return DeliveryResponse.from(delivery);
    }

    // ================================================================ helpers

    private Delivery load(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Delivery not found"));
    }

    private Delivery loadVisible(Long id, User user) {

        Delivery delivery = load(id);

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

    private Delivery loadAssignedToDriver(Long deliveryId, String email) {

        Driver driver = requireDriverProfile(accessGuard.requireUser(email));
        Delivery delivery = load(deliveryId);

        if (delivery.getDriver() == null || !delivery.getDriver().getId().equals(driver.getId())) {
            throw new ForbiddenException("Driver not assigned to this delivery");
        }

        return delivery;
    }

    private static Driver requireDriverProfile(User user) {
        if (user.getRole() != User.Role.DRIVER || user.getDriver() == null) {
            throw new BusinessRuleException("No driver profile assigned to user");
        }
        return user.getDriver();
    }

    private static void checkRestaurantOwnerOrAdmin(User user, Delivery delivery) {
        if (AccessGuard.isAdmin(user)) {
            return;
        }
        Long restaurantId = AccessGuard.ownRestaurantId(user);
        if (restaurantId == null || !restaurantId.equals(delivery.getRestaurant().getId())) {
            throw new ForbiddenException("You do not have access to this delivery");
        }
    }

    private Restaurant resolveRestaurantForCreation(User creator, Long requestedRestaurantId) {

        if (AccessGuard.isAdminOrDispatcher(creator)) {
            if (requestedRestaurantId == null) {
                throw new FieldValidationException(
                        Map.of("restaurantId", "Restaurant id is required for admin and dispatcher"));
            }
            return restaurantRepository.findById(requestedRestaurantId)
                    .orElseThrow(() -> new NotFoundException("Restaurant not found"));
        }

        if (creator.getRole() == User.Role.RESTAURANT) {
            if (creator.getRestaurant() == null) {
                throw new ForbiddenException("No restaurant is linked to your account");
            }
            // Never trust the body: a restaurant always creates for itself.
            return creator.getRestaurant();
        }

        throw new ForbiddenException("You cannot create deliveries");
    }

    private static void validateCreation(CreateDeliveryRequest request) {

        Map<String, String> errors = new LinkedHashMap<>();

        if (request.paymentMode() == Delivery.PaymentMode.CASH_ON_DELIVERY) {
            if (request.cashToCollect() == null || request.cashToCollect().signum() <= 0) {
                errors.put("cashToCollect", "Cash to collect is required and must be greater than 0 for CASH_ON_DELIVERY");
            }
        } else if (request.cashToCollect() != null && request.cashToCollect().signum() != 0) {
            errors.put("cashToCollect", "Cash to collect must be empty for PREPAID deliveries");
        }

        if ((request.deliveryLatitude() == null) != (request.deliveryLongitude() == null)) {
            errors.put("deliveryLatitude", "deliveryLatitude and deliveryLongitude must be provided together");
        }
        if ((request.pickupLatitude() == null) != (request.pickupLongitude() == null)) {
            errors.put("pickupLatitude", "pickupLatitude and pickupLongitude must be provided together");
        }

        if (!errors.isEmpty()) {
            throw new FieldValidationException(errors);
        }
    }

    /**
     * Validates and applies a status change; returns the previous status.
     */
    private static Status transition(Delivery delivery, Status next) {
        Status current = delivery.getStatus();
        if (!current.canTransitionTo(next)) {
            throw new ConflictException("Cannot change delivery status from " + current + " to " + next);
        }
        delivery.setStatus(next);
        return current;
    }

    /**
     * A driver whose last active delivery ended becomes AVAILABLE again.
     */
    private void releaseDriver(Driver driver) {
        if (driver.getAvailabilityStatus() == Driver.AvailabilityStatus.BUSY
                && !repository.existsByDriverIdAndStatusIn(driver.getId(), Status.DRIVER_ACTIVE)) {
            driver.setAvailabilityStatus(Driver.AvailabilityStatus.AVAILABLE);
            driverRepository.save(driver);
        }
    }

    private void record(Delivery delivery, Event event, Status oldStatus, Status newStatus, String email, String note) {
        historyRepository.save(DeliveryStatusHistory.builder()
                .delivery(delivery)
                .event(event)
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .changedBy(email)
                .note(note)
                .build());
    }

    private static void addCommonFilters(
            List<Specification<Delivery>> filters,
            List<Status> statuses,
            String from,
            String to
    ) {
        if (statuses != null && !statuses.isEmpty()) {
            filters.add(DeliverySpecifications.statusIn(statuses));
        }

        LocalDateTime fromBound = parseBound(from, false, "from");
        LocalDateTime toBound = parseBound(to, true, "to");

        if (fromBound != null && toBound != null && !fromBound.isBefore(toBound)) {
            throw new FieldValidationException(Map.of("to", "'to' must be after 'from'"));
        }
        if (fromBound != null) {
            filters.add(DeliverySpecifications.createdAtFrom(fromBound));
        }
        if (toBound != null) {
            filters.add(DeliverySpecifications.createdAtBefore(toBound));
        }
    }

    /**
     * Accepts a plain date ({@code 2026-10-15}, Europe/Brussels; as 'to' it
     * includes the whole day) or an ISO-8601 date-time with offset
     * ({@code 2026-10-15T08:00:00Z}). Returns server-local time.
     */
    static LocalDateTime parseBound(String value, boolean upper, String field) {

        if (value == null || value.isBlank()) {
            return null;
        }

        String trimmed = value.trim();
        ZonedDateTime instant;

        try {
            if (trimmed.length() == 10) {
                LocalDate date = LocalDate.parse(trimmed);
                instant = (upper ? date.plusDays(1) : date).atStartOfDay(BUSINESS_ZONE);
            } else {
                instant = OffsetDateTime.parse(trimmed).toZonedDateTime();
            }
        } catch (DateTimeParseException e) {
            throw new FieldValidationException(Map.of(field,
                    "Must be a date (2026-10-15) or an ISO-8601 date-time with offset (2026-10-15T08:00:00Z)"));
        }

        return instant.withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
    }

    private static String restaurantAddress(Restaurant restaurant) {
        String city = trimToNull(restaurant.getCity());
        return city == null ? restaurant.getAddress() : restaurant.getAddress() + ", " + city;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static BigDecimal coordinate(BigDecimal value) {
        return value == null ? null : value.setScale(7, RoundingMode.HALF_UP);
    }

    private static BigDecimal money(BigDecimal value) {
        return value == null ? null : value.setScale(2, RoundingMode.HALF_UP);
    }
}
