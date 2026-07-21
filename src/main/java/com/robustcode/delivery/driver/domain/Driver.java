package com.robustcode.delivery.driver.domain;


import java.time.LocalDateTime;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import com.robustcode.delivery.user.domain.User;
import com.robustcode.delivery.vehicle.domain.Vehicle;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;



@Entity
@Table(name = "drivers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({
        "hibernateLazyInitializer",
        "handler"
})
public class Driver {



    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;




    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            unique = true
    )
    @JsonIgnore
    private User user;




    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "vehicle_id",
            unique = true
    )
    @JsonIgnore
    private Vehicle vehicle;




    @Column(
            name = "phone_number",
            nullable = false
    )
    private String phoneNumber;




    /*
     * Gardé pour compatibilité avec la table actuelle.
     * À supprimer plus tard avec une migration Flyway.
     */
    @Column(name = "vehicle_type")
    private String vehicleType;




    /*
     * Gardé pour compatibilité avec la table actuelle.
     * À supprimer plus tard avec une migration Flyway.
     */
    @Column(name = "vehicle_plate")
    private String vehiclePlate;




    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(
            name = "availability_status",
            nullable = false
    )
    private AvailabilityStatus availabilityStatus =
            AvailabilityStatus.OFFLINE;




    @Builder.Default
    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt =
            LocalDateTime.now();




    @Column(name = "updated_at")
    private LocalDateTime updatedAt;




    @PrePersist
    protected void onCreate(){

        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();

    }




    @PreUpdate
    protected void onUpdate(){

        updatedAt = LocalDateTime.now();

    }





    public enum AvailabilityStatus {


        AVAILABLE,


        BUSY,


        OFFLINE


    }


}