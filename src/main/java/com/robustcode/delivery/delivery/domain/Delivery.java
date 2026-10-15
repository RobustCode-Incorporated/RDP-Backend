package com.robustcode.delivery.delivery.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import com.robustcode.delivery.driver.domain.Driver;
import com.robustcode.delivery.restaurant.domain.Restaurant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Core entity of the platform: one restaurant-to-customer delivery.
 *
 * <p>The end customer has no account: they are described by
 * {@link #customerName} / {@link #customerPhone}. Status changes must go
 * through {@code DeliveryService} so they are validated against
 * {@link Status#canTransitionTo(Status)} and recorded in
 * {@code delivery_status_history}.</p>
 *
 * <p>Never serialized directly: controllers return {@code DeliveryResponse}.</p>
 */
@Entity
@Table(name = "deliveries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Delivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Optimistic locking: concurrent updates of the same delivery fail with 409. */
    @Version
    @Column(nullable = false)
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id")
    private Driver driver;

    // ------------------------------------------------------------ customer

    @Column(name = "customer_name", nullable = false, length = 150)
    private String customerName;

    @Column(name = "customer_phone", length = 30)
    private String customerPhone;

    // ------------------------------------------------------------ addresses

    @Column(name = "pickup_address", nullable = false)
    private String pickupAddress;

    @Column(name = "pickup_latitude", precision = 10, scale = 7)
    private BigDecimal pickupLatitude;

    @Column(name = "pickup_longitude", precision = 10, scale = 7)
    private BigDecimal pickupLongitude;

    @Column(name = "delivery_address", nullable = false)
    private String deliveryAddress;

    @Column(name = "delivery_latitude", precision = 10, scale = 7)
    private BigDecimal deliveryLatitude;

    @Column(name = "delivery_longitude", precision = 10, scale = 7)
    private BigDecimal deliveryLongitude;

    // ------------------------------------------------------------ order info

    /** What is delivered (e.g. "2x Poulet DG, 1x Alloco"). */
    @Column(length = 500)
    private String description;

    /** Instructions for the driver (door code, floor, ...). */
    @Column(length = 500)
    private String notes;

    /** The restaurant's own order reference. */
    @Column(name = "order_reference", length = 100)
    private String orderReference;

    /** Order total in EUR (informational). */
    @Column(name = "order_amount", precision = 10, scale = 2)
    private BigDecimal orderAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_mode", nullable = false, length = 30)
    private PaymentMode paymentMode;

    /** Cash (EUR) the driver must collect; only for CASH_ON_DELIVERY. */
    @Column(name = "cash_to_collect", precision = 10, scale = 2)
    private BigDecimal cashToCollect;

    /** Delivery fee (EUR) charged to the restaurant. */
    @Column(name = "delivery_fee", precision = 10, scale = 2)
    private BigDecimal deliveryFee;

    // ------------------------------------------------------------ lifecycle

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Status status = Status.CREATED;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;

    @Column(name = "ready_at")
    private LocalDateTime readyAt;

    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;

    @Column(name = "picked_up_at")
    private LocalDateTime pickedUpAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "failed_at")
    private LocalDateTime failedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum PaymentMode {
        PREPAID,
        CASH_ON_DELIVERY
    }

    public enum Status {

        CREATED,
        ASSIGNED,
        PICKED_UP,
        IN_TRANSIT,
        DELIVERED,
        FAILED,
        CANCELLED;

        private static final Map<Status, Set<Status>> ALLOWED = Map.of(
                CREATED, EnumSet.of(ASSIGNED, CANCELLED),
                // ASSIGNED -> ASSIGNED is a reassignment before pickup
                ASSIGNED, EnumSet.of(ASSIGNED, PICKED_UP, FAILED, CANCELLED),
                PICKED_UP, EnumSet.of(IN_TRANSIT, FAILED),
                IN_TRANSIT, EnumSet.of(DELIVERED, FAILED),
                DELIVERED, EnumSet.noneOf(Status.class),
                FAILED, EnumSet.noneOf(Status.class),
                CANCELLED, EnumSet.noneOf(Status.class)
        );

        /** Statuses during which the assigned driver is busy with the delivery. */
        public static final Set<Status> DRIVER_ACTIVE = EnumSet.of(ASSIGNED, PICKED_UP, IN_TRANSIT);

        public boolean canTransitionTo(Status next) {
            return ALLOWED.get(this).contains(next);
        }

        public boolean isTerminal() {
            return ALLOWED.get(this).isEmpty();
        }
    }
}
