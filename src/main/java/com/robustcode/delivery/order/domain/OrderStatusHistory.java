package com.robustcode.delivery.order.domain;


import jakarta.persistence.*;

import lombok.*;

import java.time.LocalDateTime;


@Entity
@Table(name = "order_status_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderStatusHistory {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;



    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "order_id",
            nullable = false
    )
    private Order order;



    @Enumerated(EnumType.STRING)
    private Order.Status oldStatus;



    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Order.Status newStatus;



    @Column(nullable = false)
    private String changedBy;



    @Column(nullable = false)
    private LocalDateTime createdAt;



    @PrePersist
    protected void onCreate(){

        createdAt = LocalDateTime.now();

    }

}