package com.robustcode.delivery.restaurant.domain;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.robustcode.delivery.user.domain.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "restaurants")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Restaurant {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToMany(
            mappedBy = "restaurant",
            fetch = FetchType.LAZY
    )
    @JsonIgnore
    private List<User> users;


    @Column(nullable = false, length = 150)
    private String name;


    @Column(length = 500)
    private String description;


    @Column(name = "phone_number", length = 30)
    private String phoneNumber;


    @Column(length = 255)
    private String email;


    @Column(nullable = false)
    private String address;


    private String city;


    private String country;


    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.ACTIVE;


    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;


    private LocalDateTime updatedAt;



    @PrePersist
    protected void onCreate(){

        createdAt = LocalDateTime.now();

        if(status == null){
            status = Status.ACTIVE;
        }

    }



    @PreUpdate
    protected void onUpdate(){

        updatedAt = LocalDateTime.now();

    }



    public enum Status {

        ACTIVE,
        INACTIVE

    }

}