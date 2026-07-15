package com.robustcode.delivery.vehicle.domain;


import java.time.LocalDateTime;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.robustcode.delivery.driver.domain.Driver;


import jakarta.persistence.*;
import lombok.*;



@Entity
@Table(name = "vehicles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({
        "hibernateLazyInitializer",
        "handler"
})
public class Vehicle {



    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;



    @Column(
        name = "vehicle_type",
        nullable = false
    )
    private String vehicleType;



    @Column(
        name = "plate_number",
        nullable = false,
        unique = true
    )
    private String plateNumber;



    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;



    @OneToOne(
        mappedBy = "vehicle",
        fetch = FetchType.LAZY
    )
    @JsonIgnore
    private Driver driver;



    @Builder.Default
    @Column(
        name = "created_at",
        nullable = false,
        updatable = false
    )
    private LocalDateTime createdAt =
            LocalDateTime.now();



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




    public enum Status {


        AVAILABLE,

        ASSIGNED,

        MAINTENANCE


    }

}