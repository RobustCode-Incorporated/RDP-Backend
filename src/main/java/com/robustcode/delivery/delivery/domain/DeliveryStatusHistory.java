package com.robustcode.delivery.delivery.domain;

import java.time.LocalDateTime;

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
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One entry per status transition or lifecycle event of a delivery.
 * For events that do not change the status (READY, REASSIGNED)
 * {@code oldStatus == newStatus}.
 */
@Entity
@Table(name = "delivery_status_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_id", nullable = false)
    private Delivery delivery;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Event event;

    @Enumerated(EnumType.STRING)
    @Column(name = "old_status", length = 30)
    private Delivery.Status oldStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false, length = 30)
    private Delivery.Status newStatus;

    /** Email of the user who triggered the change. */
    @Column(name = "changed_by", nullable = false)
    private String changedBy;

    @Column(length = 500)
    private String note;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public enum Event {
        CREATED,
        ASSIGNED,
        REASSIGNED,
        READY,
        PICKED_UP,
        IN_TRANSIT,
        DELIVERED,
        FAILED,
        CANCELLED,
        /** Synthetic entry written by the V6 migration for pre-existing deliveries. */
        MIGRATED
    }
}
