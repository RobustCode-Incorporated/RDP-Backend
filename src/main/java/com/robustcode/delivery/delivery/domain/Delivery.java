package com.robustcode.delivery.delivery.domain;


import java.math.BigDecimal;
import java.time.LocalDateTime;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import com.robustcode.delivery.driver.domain.Driver;
import com.robustcode.delivery.user.domain.User;
import com.robustcode.delivery.restaurant.domain.Restaurant;


import jakarta.persistence.*;

import lombok.*;



@Entity
@Table(name = "deliveries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({
        "hibernateLazyInitializer",
        "handler"
})
public class Delivery {



    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;




    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "customer_id",
            nullable = false
    )
    @JsonIgnore
    private User customer;




    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "driver_id"
    )
    @JsonIgnoreProperties({
            "user",
            "vehicle"
    })
    private Driver driver;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "restaurant_id",
            nullable = false
    )
    @JsonIgnoreProperties({
            "hibernateLazyInitializer",
            "handler"
    })
    private Restaurant restaurant;




    @Column(
            name = "pickup_address",
            nullable = false
    )
    private String pickupAddress;




    @Column(
            name = "delivery_address",
            nullable = false
    )
    private String deliveryAddress;




    private String description;




    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.PENDING;




    private BigDecimal price;




    @Column(
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;




    private LocalDateTime updatedAt;





    @PrePersist
    protected void onCreate(){

        createdAt = LocalDateTime.now();

    }





    @PreUpdate
    protected void onUpdate(){

        updatedAt = LocalDateTime.now();

    }






    public enum Status {


        PENDING,


        ASSIGNED,


        PICKED_UP,


        IN_TRANSIT,


        DELIVERED,


        CANCELLED


    }


}