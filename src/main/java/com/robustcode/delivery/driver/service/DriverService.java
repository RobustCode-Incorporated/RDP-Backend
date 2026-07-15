package com.robustcode.delivery.driver.service;


import java.util.List;


import com.robustcode.delivery.driver.domain.Driver;
import com.robustcode.delivery.driver.dto.CreateDriverRequest;
import com.robustcode.delivery.driver.dto.DriverResponse;
import com.robustcode.delivery.driver.repository.DriverRepository;

import com.robustcode.delivery.user.domain.User;
import com.robustcode.delivery.user.repository.UserRepository;

import com.robustcode.delivery.vehicle.domain.Vehicle;
import com.robustcode.delivery.vehicle.repository.VehicleRepository;


import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;



@Service
@Transactional
@RequiredArgsConstructor
public class DriverService {



    private final DriverRepository driverRepository;

    private final UserRepository userRepository;

    private final VehicleRepository vehicleRepository;




    public DriverResponse createDriver(
            CreateDriverRequest request
    ){


        User user = userRepository.findById(request.userId())

                .orElseThrow(() ->
                        new RuntimeException(
                                "User not found"
                        )
                );



        if(user.getRole() != User.Role.DRIVER){

            throw new RuntimeException(
                    "User must have DRIVER role"
            );

        }



        if(driverRepository.existsByUser(user)){


            throw new RuntimeException(
                    "Driver profile already exists"
            );

        }



        if(vehicleRepository.existsByPlateNumber(
                request.vehiclePlate()
        )){

            throw new RuntimeException(
                    "Vehicle plate already exists"
            );

        }



        Vehicle vehicle = Vehicle.builder()

                .vehicleType(
                        request.vehicleType()
                )

                .plateNumber(
                        request.vehiclePlate()
                )

                .status(
                        Vehicle.Status.ASSIGNED
                )

                .build();



        Vehicle savedVehicle =
                vehicleRepository.save(vehicle);




        Driver driver = Driver.builder()

                .user(user)

                .vehicle(savedVehicle)

                .phoneNumber(
                        request.phoneNumber()
                )

                .vehicleType(
                        request.vehicleType()
                )

                .vehiclePlate(
                        request.vehiclePlate()
                )

                .availabilityStatus(
                        Driver.AvailabilityStatus.OFFLINE
                )

                .build();



        Driver savedDriver =
                driverRepository.save(driver);



        return mapToResponse(savedDriver);

    }




    @Transactional(readOnly = true)
    public DriverResponse findMyDriver(
            String email
    ){


        User user = userRepository.findByEmail(email)

                .orElseThrow(() ->
                        new RuntimeException(
                                "User not found"
                        )
                );



        if(user.getDriver() == null){

            throw new RuntimeException(
                    "No driver profile assigned to user"
            );

        }

        return mapToResponse(
                user.getDriver()
        );

    }



        @Transactional(readOnly = true)
        public List<DriverResponse> findAll(){

                return driverRepository.findAll()
                                .stream()
                                .map(this::mapToResponse)
                                .toList();

        }





    private DriverResponse mapToResponse(
            Driver driver
    ){


        return new DriverResponse(

                driver.getId(),

                driver.getUser().getId(),

                driver.getUser().getFirstName(),

                driver.getUser().getLastName(),

                driver.getUser().getEmail(),

                driver.getPhoneNumber(),

                driver.getVehicleType(),

                driver.getVehiclePlate(),

                driver.getAvailabilityStatus()

        );

    }


}