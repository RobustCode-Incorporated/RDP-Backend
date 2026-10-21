package com.robustcode.delivery.delivery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.robustcode.delivery.delivery.domain.Delivery;
import com.robustcode.delivery.delivery.repository.DeliveryRepository;
import com.robustcode.delivery.delivery.repository.DeliveryStatusHistoryRepository;
import com.robustcode.delivery.driver.domain.Driver;
import com.robustcode.delivery.driver.repository.DriverRepository;
import com.robustcode.delivery.restaurant.domain.Restaurant;
import com.robustcode.delivery.restaurant.repository.RestaurantRepository;
import com.robustcode.delivery.user.domain.User;
import com.robustcode.delivery.user.repository.UserRepository;
import com.robustcode.delivery.vehicle.repository.VehicleRepository;

/**
 * End-to-end delivery lifecycle through the real security filter chain:
 * restaurant creates -> admin/dispatcher assigns -> driver picks up,
 * starts, completes or fails; cancellations; ownership rules.
 */
@SpringBootTest
@AutoConfigureMockMvc
class DeliveryFlowIntegrationTest {

    private static final String PASSWORD = "correct-horse-battery";

    private static final String ADMIN = "admin@rdp.test";
    private static final String DISPATCHER = "dispatcher@rdp.test";
    private static final String OWNER_A = "owner-a@rdp.test";
    private static final String OWNER_B = "owner-b@rdp.test";
    private static final String DRIVER_1 = "driver1@rdp.test";
    private static final String DRIVER_2 = "driver2@rdp.test";
    private static final String DRIVER_OFF = "driver-disabled@rdp.test";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private UserRepository userRepository;
    @Autowired private RestaurantRepository restaurantRepository;
    @Autowired private DriverRepository driverRepository;
    @Autowired private VehicleRepository vehicleRepository;
    @Autowired private DeliveryRepository deliveryRepository;
    @Autowired private DeliveryStatusHistoryRepository historyRepository;

    private final Map<String, String> tokens = new HashMap<>();

    private Restaurant restaurantA;
    private Restaurant restaurantB;
    private Driver driver1;
    private Driver driver2;
    private Driver disabledDriver;

    @BeforeEach
    void setUp() {
        historyRepository.deleteAll();
        deliveryRepository.deleteAll();
        driverRepository.deleteAll();
        vehicleRepository.deleteAll();
        userRepository.deleteAll();
        restaurantRepository.deleteAll();
        tokens.clear();

        restaurantA = restaurantRepository.save(Restaurant.builder()
                .name("Chez Mama Africa")
                .email("a@resto.test")
                .phoneNumber("+32 2 111 11 11")
                .address("Chaussée de Wavre 100")
                .city("Ixelles")
                .status(Restaurant.Status.ACTIVE)
                .build());
        restaurantB = restaurantRepository.save(Restaurant.builder()
                .name("Le Baobab")
                .email("b@resto.test")
                .address("Rue Haute 5")
                .status(Restaurant.Status.ACTIVE)
                .build());

        userRepository.save(user(ADMIN, User.Role.ADMIN, null, true));
        userRepository.save(user(DISPATCHER, User.Role.DISPATCHER, null, true));
        userRepository.save(user(OWNER_A, User.Role.RESTAURANT, restaurantA, true));
        userRepository.save(user(OWNER_B, User.Role.RESTAURANT, restaurantB, true));

        driver1 = driver(userRepository.save(user(DRIVER_1, User.Role.DRIVER, null, true)));
        driver2 = driver(userRepository.save(user(DRIVER_2, User.Role.DRIVER, null, true)));
        disabledDriver = driver(userRepository.save(user(DRIVER_OFF, User.Role.DRIVER, null, false)));
    }

    // ================================================================ happy path

    @Test
    void fullHappyPathRecordsHistoryAndKeepsDriverAvailabilityConsistent() throws Exception {

        // Admin sets the restaurant coordinates: they become the default pickup point
        perform(patch("/api/restaurants/{id}", restaurantA.getId()), ADMIN,
                Map.of("latitude", 50.8337, "longitude", 4.3713))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.latitude").value(50.8337))
                .andExpect(jsonPath("$.longitude").value(4.3713));

        Map<String, Object> body = validBody();
        body.put("paymentMode", "CASH_ON_DELIVERY");
        body.put("cashToCollect", 25.50);
        body.put("orderAmount", 25.50);
        body.put("deliveryFee", 4.00);
        body.put("orderReference", "WA-0042");
        body.put("notes", "Code porte 1234, 3e étage");
        body.put("restaurantId", restaurantB.getId()); // ignored for a restaurant user

        JsonNode created = json(perform(post("/api/deliveries"), OWNER_A, body)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.restaurant.id").value(restaurantA.getId()))
                .andExpect(jsonPath("$.restaurant.name").value("Chez Mama Africa"))
                .andExpect(jsonPath("$.restaurant.phone").value("+32 2 111 11 11"))
                .andExpect(jsonPath("$.pickupAddress").value("Chaussée de Wavre 100, Ixelles"))
                .andExpect(jsonPath("$.pickupLatitude").value(50.8337))
                .andExpect(jsonPath("$.pickupLongitude").value(4.3713))
                .andExpect(jsonPath("$.customerName").value("Awa Diop"))
                .andExpect(jsonPath("$.customerPhone").value("+32 470 12 34 56"))
                .andExpect(jsonPath("$.paymentMode").value("CASH_ON_DELIVERY"))
                .andExpect(jsonPath("$.cashToCollect").value(25.5))
                .andExpect(jsonPath("$.orderReference").value("WA-0042"))
                .andExpect(jsonPath("$.driver").value(nullValue()))
                .andExpect(jsonPath("$.createdAt").value(notNullValue()))
                .andExpect(jsonPath("$.version").value(0)));
        long id = created.get("id").asLong();

        // Dispatch view lists both available drivers (disabled driver excluded)
        perform(get("/api/drivers/available"), DISPATCHER, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        perform(put("/api/deliveries/{id}/assign", id), DISPATCHER, Map.of("driverId", driver1.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ASSIGNED"))
                .andExpect(jsonPath("$.driver.id").value(driver1.getId()))
                .andExpect(jsonPath("$.driver.firstName").value("First"))
                .andExpect(jsonPath("$.driver.phone").value("+32 400 00 00 00"))
                .andExpect(jsonPath("$.assignedAt").value(notNullValue()));

        assertAvailability(driver1, Driver.AvailabilityStatus.BUSY);
        perform(get("/api/drivers/available"), ADMIN, null)
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(driver2.getId()));

        perform(patch("/api/deliveries/{id}/ready", id), OWNER_A, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ASSIGNED"))
                .andExpect(jsonPath("$.readyAt").value(notNullValue()));

        perform(get("/api/deliveries/me"), DRIVER_1, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(id))
                .andExpect(jsonPath("$[0].customerPhone").value("+32 470 12 34 56"))
                .andExpect(jsonPath("$[0].cashToCollect").value(25.5));

        perform(put("/api/deliveries/{id}/pickup", id), DRIVER_1, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PICKED_UP"))
                .andExpect(jsonPath("$.pickedUpAt").value(notNullValue()));

        perform(put("/api/deliveries/{id}/start", id), DRIVER_1, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_TRANSIT"));

        assertAvailability(driver1, Driver.AvailabilityStatus.BUSY);

        perform(put("/api/deliveries/{id}/complete", id), DRIVER_1, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DELIVERED"))
                .andExpect(jsonPath("$.deliveredAt").value(notNullValue()));

        assertAvailability(driver1, Driver.AvailabilityStatus.AVAILABLE);

        // Restaurant follows the full timeline
        perform(get("/api/deliveries/{id}/history", id), OWNER_A, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(6)))
                .andExpect(jsonPath("$[0].event").value("CREATED"))
                .andExpect(jsonPath("$[0].oldStatus").value(nullValue()))
                .andExpect(jsonPath("$[0].newStatus").value("CREATED"))
                .andExpect(jsonPath("$[0].changedBy").value(OWNER_A))
                .andExpect(jsonPath("$[1].event").value("ASSIGNED"))
                .andExpect(jsonPath("$[1].oldStatus").value("CREATED"))
                .andExpect(jsonPath("$[1].newStatus").value("ASSIGNED"))
                .andExpect(jsonPath("$[1].changedBy").value(DISPATCHER))
                .andExpect(jsonPath("$[2].event").value("READY"))
                .andExpect(jsonPath("$[2].oldStatus").value("ASSIGNED"))
                .andExpect(jsonPath("$[2].newStatus").value("ASSIGNED"))
                .andExpect(jsonPath("$[3].event").value("PICKED_UP"))
                .andExpect(jsonPath("$[3].changedBy").value(DRIVER_1))
                .andExpect(jsonPath("$[4].event").value("IN_TRANSIT"))
                .andExpect(jsonPath("$[5].event").value("DELIVERED"))
                .andExpect(jsonPath("$[5].oldStatus").value("IN_TRANSIT"))
                .andExpect(jsonPath("$[5].newStatus").value("DELIVERED"));

        perform(get("/api/deliveries/{id}", id), ADMIN, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DELIVERED"));

        // Terminal state
        perform(put("/api/deliveries/{id}/fail", id), DRIVER_1, Map.of("reason", "too late"))
                .andExpect(status().isConflict());
    }

    @Test
    void adminCreatesForAGivenRestaurantAndMustProvideIt() throws Exception {
        Map<String, Object> body = validBody();

        perform(post("/api/deliveries"), ADMIN, body)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.restaurantId").exists());

        body.put("restaurantId", restaurantB.getId());
        body.put("pickupAddress", "Back door, Rue Haute 5");
        perform(post("/api/deliveries"), DISPATCHER, body)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.restaurant.id").value(restaurantB.getId()))
                .andExpect(jsonPath("$.pickupAddress").value("Back door, Rue Haute 5"))
                .andExpect(jsonPath("$.pickupLatitude").value(nullValue()));

        body.put("restaurantId", 999_999);
        perform(post("/api/deliveries"), ADMIN, body)
                .andExpect(status().isNotFound());
    }

    // ================================================================ fail path

    @Test
    void driverFailsDeliveryWithReasonAndBecomesAvailable() throws Exception {
        long id = createAndAssign(OWNER_A, driver1);

        perform(put("/api/deliveries/{id}/pickup", id), DRIVER_1, null).andExpect(status().isOk());

        perform(put("/api/deliveries/{id}/fail", id), DRIVER_1, Map.of("reason", "  "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.reason").exists());

        perform(put("/api/deliveries/{id}/fail", id), DRIVER_1, Map.of("reason", "Client injoignable"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.failureReason").value("Client injoignable"))
                .andExpect(jsonPath("$.failedAt").value(notNullValue()))
                .andExpect(jsonPath("$.description").value(nullValue()));

        assertAvailability(driver1, Driver.AvailabilityStatus.AVAILABLE);

        perform(get("/api/deliveries/{id}/history", id), OWNER_A, null)
                .andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$[3].event").value("FAILED"))
                .andExpect(jsonPath("$[3].oldStatus").value("PICKED_UP"))
                .andExpect(jsonPath("$[3].note").value("Client injoignable"));

        perform(put("/api/deliveries/{id}/complete", id), DRIVER_1, null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Cannot change delivery status from FAILED to DELIVERED"));

        perform(get("/api/deliveries").param("status", "FAILED"), OWNER_A, null)
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void outOfOrderDriverActionsAreConflicts() throws Exception {
        long id = createAndAssign(OWNER_A, driver1);

        perform(put("/api/deliveries/{id}/start", id), DRIVER_1, null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
        perform(put("/api/deliveries/{id}/complete", id), DRIVER_1, null)
                .andExpect(status().isConflict());
    }

    // ================================================================ cancel

    @Test
    void restaurantCancelsCreatedAndAssignedDeliveries() throws Exception {
        long created = createDelivery(OWNER_A);

        perform(patch("/api/deliveries/{id}/cancel", created), OWNER_A, Map.of("reason", "Client a annulé"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.cancellationReason").value("Client a annulé"))
                .andExpect(jsonPath("$.cancelledAt").value(notNullValue()));

        // Assigned: cancelling frees the driver; the body is optional
        long assigned = createAndAssign(OWNER_A, driver1);
        assertAvailability(driver1, Driver.AvailabilityStatus.BUSY);

        perform(patch("/api/deliveries/{id}/cancel", assigned), ADMIN, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.cancellationReason").value(nullValue()));
        assertAvailability(driver1, Driver.AvailabilityStatus.AVAILABLE);

        perform(get("/api/deliveries/{id}/history", assigned), ADMIN, null)
                .andExpect(jsonPath("$[2].event").value("CANCELLED"))
                .andExpect(jsonPath("$[2].oldStatus").value("ASSIGNED"))
                .andExpect(jsonPath("$[2].changedBy").value(ADMIN));

        // The driver can no longer act on it
        perform(put("/api/deliveries/{id}/pickup", assigned), DRIVER_1, null)
                .andExpect(status().isConflict());
    }

    @Test
    void cannotCancelAfterPickup() throws Exception {
        long id = createAndAssign(OWNER_A, driver1);
        perform(put("/api/deliveries/{id}/pickup", id), DRIVER_1, null).andExpect(status().isOk());

        perform(patch("/api/deliveries/{id}/cancel", id), OWNER_A, Map.of("reason", "too late"))
                .andExpect(status().isConflict());
        perform(patch("/api/deliveries/{id}/ready", id), OWNER_A, null)
                .andExpect(status().isConflict());
    }

    @Test
    void dispatcherAndDriverCannotCancel() throws Exception {
        long id = createAndAssign(OWNER_A, driver1);

        perform(patch("/api/deliveries/{id}/cancel", id), DISPATCHER, null)
                .andExpect(status().isForbidden());
        perform(patch("/api/deliveries/{id}/cancel", id), DRIVER_1, null)
                .andExpect(status().isForbidden());
    }

    // ================================================================ restaurant isolation

    @Test
    void restaurantCannotSeeOrActOnAnotherRestaurantsDeliveries() throws Exception {
        long deliveryOfA = createDelivery(OWNER_A);
        long deliveryOfB = createDelivery(OWNER_B);

        perform(get("/api/deliveries"), OWNER_B, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(deliveryOfB));

        perform(get("/api/deliveries/{id}", deliveryOfA), OWNER_B, null)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You do not have access to this delivery"));
        perform(get("/api/deliveries/{id}/history", deliveryOfA), OWNER_B, null)
                .andExpect(status().isForbidden());
        perform(patch("/api/deliveries/{id}/cancel", deliveryOfA), OWNER_B, Map.of("reason", "nope"))
                .andExpect(status().isForbidden());
        perform(patch("/api/deliveries/{id}/ready", deliveryOfA), OWNER_B, null)
                .andExpect(status().isForbidden());
        perform(get("/api/restaurants/{id}/deliveries", restaurantA.getId()), OWNER_B, null)
                .andExpect(status().isForbidden());

        perform(get("/api/deliveries/{id}", deliveryOfA), OWNER_A, null).andExpect(status().isOk());
        perform(get("/api/restaurants/{id}/deliveries", restaurantA.getId()), OWNER_A, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        assertThat(deliveryRepository.findById(deliveryOfA).orElseThrow().getStatus())
                .isEqualTo(Delivery.Status.CREATED);

        perform(get("/api/deliveries/{id}", 999_999L), ADMIN, null)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Delivery not found"));
    }

    @Test
    void restaurantCannotAssignDrivers() throws Exception {
        long id = createDelivery(OWNER_A);
        perform(put("/api/deliveries/{id}/assign", id), OWNER_A, Map.of("driverId", driver1.getId()))
                .andExpect(status().isForbidden());
        perform(get("/api/drivers/available"), OWNER_A, null)
                .andExpect(status().isForbidden());
    }

    // ================================================================ driver rules

    @Test
    void driverCannotActOnDeliveryNotAssignedToThem() throws Exception {
        long unassigned = createDelivery(OWNER_A);
        long assignedToDriver1 = createAndAssign(OWNER_A, driver1);

        for (String action : new String[] {"pickup", "start", "complete"}) {
            perform(put("/api/deliveries/{id}/" + action, unassigned), DRIVER_2, null)
                    .andExpect(status().isForbidden());
            perform(put("/api/deliveries/{id}/" + action, assignedToDriver1), DRIVER_2, null)
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value("Driver not assigned to this delivery"));
        }
        perform(put("/api/deliveries/{id}/fail", assignedToDriver1), DRIVER_2, Map.of("reason", "x"))
                .andExpect(status().isForbidden());

        perform(get("/api/deliveries/{id}", assignedToDriver1), DRIVER_2, null)
                .andExpect(status().isForbidden());
        perform(get("/api/deliveries"), DRIVER_2, null)
                .andExpect(jsonPath("$", hasSize(0)));
        perform(get("/api/deliveries"), DRIVER_1, null)
                .andExpect(jsonPath("$", hasSize(1)));

        assertThat(deliveryRepository.findById(assignedToDriver1).orElseThrow().getStatus())
                .isEqualTo(Delivery.Status.ASSIGNED);
    }

    // ================================================================ assignment rules

    @Test
    void assignmentRequiresAnAvailableEnabledDriver() throws Exception {
        createAndAssign(OWNER_A, driver1);
        long second = createDelivery(OWNER_A);

        perform(put("/api/deliveries/{id}/assign", second), ADMIN, Map.of("driverId", driver1.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Driver is not available. Current availability: BUSY"));

        perform(put("/api/deliveries/{id}/assign", second), ADMIN, Map.of("driverId", disabledDriver.getId()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Driver account is disabled"));

        perform(put("/api/deliveries/{id}/assign", second), ADMIN, Map.of("driverId", 999_999))
                .andExpect(status().isNotFound());

        perform(put("/api/deliveries/{id}/assign", second), ADMIN, Map.of())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.driverId").exists());
    }

    @Test
    void reassignmentBeforePickupFreesThePreviousDriver() throws Exception {
        long id = createAndAssign(OWNER_A, driver1);

        // Same driver again: no-op
        perform(put("/api/deliveries/{id}/assign", id), ADMIN, Map.of("driverId", driver1.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.driver.id").value(driver1.getId()));

        perform(put("/api/deliveries/{id}/assign", id), ADMIN, Map.of("driverId", driver2.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ASSIGNED"))
                .andExpect(jsonPath("$.driver.id").value(driver2.getId()));

        assertAvailability(driver1, Driver.AvailabilityStatus.AVAILABLE);
        assertAvailability(driver2, Driver.AvailabilityStatus.BUSY);

        perform(get("/api/deliveries/{id}/history", id), ADMIN, null)
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[2].event").value("REASSIGNED"))
                .andExpect(jsonPath("$[2].oldStatus").value("ASSIGNED"))
                .andExpect(jsonPath("$[2].newStatus").value("ASSIGNED"));

        // Old driver lost access
        perform(put("/api/deliveries/{id}/pickup", id), DRIVER_1, null)
                .andExpect(status().isForbidden());

        perform(put("/api/deliveries/{id}/pickup", id), DRIVER_2, null).andExpect(status().isOk());

        // No reassignment after pickup
        perform(put("/api/deliveries/{id}/assign", id), ADMIN, Map.of("driverId", driver1.getId()))
                .andExpect(status().isConflict());
    }

    @Test
    void staleUpdateIsRejectedByOptimisticLocking() throws Exception {
        long id = createDelivery(OWNER_A);
        Delivery stale = deliveryRepository.findById(id).orElseThrow();

        perform(put("/api/deliveries/{id}/assign", id), ADMIN, Map.of("driverId", driver1.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(1));

        stale.setStatus(Delivery.Status.CANCELLED);
        assertThatThrownBy(() -> deliveryRepository.save(stale))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);
    }

    // ================================================================ validation

    @Test
    void cashOnDeliveryRequiresPositiveCashToCollect() throws Exception {
        Map<String, Object> body = validBody();
        body.put("paymentMode", "CASH_ON_DELIVERY");

        perform(post("/api/deliveries"), OWNER_A, body)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.cashToCollect").exists());

        body.put("cashToCollect", 0);
        perform(post("/api/deliveries"), OWNER_A, body)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.cashToCollect").exists());

        body.put("cashToCollect", 18.5);
        perform(post("/api/deliveries"), OWNER_A, body)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cashToCollect").value(18.5));

        Map<String, Object> prepaid = validBody();
        prepaid.put("cashToCollect", 10);
        perform(post("/api/deliveries"), OWNER_A, prepaid)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.cashToCollect").value("Cash to collect must be empty for PREPAID deliveries"));
    }

    @Test
    void requiredFieldsAndPhoneFormatAreValidated() throws Exception {
        perform(post("/api/deliveries"), OWNER_A, Map.of())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.path").value("/api/deliveries"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.fieldErrors.customerName").exists())
                .andExpect(jsonPath("$.fieldErrors.customerPhone").exists())
                .andExpect(jsonPath("$.fieldErrors.deliveryAddress").exists())
                .andExpect(jsonPath("$.fieldErrors.paymentMode").exists());

        for (String bad : new String[] {"abc", "12345", "+32 470 12 34 56 78 90 12 34"}) {
            Map<String, Object> body = validBody();
            body.put("customerPhone", bad);
            perform(post("/api/deliveries"), OWNER_A, body)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.customerPhone").exists());
        }

        for (String good : new String[] {"0470 12 34 56", "0470/12.34.56", "+32470123456", "0032 2 123 45 67", "+33 6 12 34 56 78"}) {
            Map<String, Object> body = validBody();
            body.put("customerPhone", good);
            perform(post("/api/deliveries"), OWNER_A, body).andExpect(status().isCreated());
        }

        Map<String, Object> halfCoordinates = validBody();
        halfCoordinates.put("deliveryLatitude", 50.85);
        perform(post("/api/deliveries"), OWNER_A, halfCoordinates)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.deliveryLatitude").exists());

        Map<String, Object> badEnum = validBody();
        badEnum.put("paymentMode", "CARD");
        perform(post("/api/deliveries"), OWNER_A, badEnum)
                .andExpect(status().isBadRequest());
    }

    // ================================================================ filters

    @Test
    void listCanBeFilteredByStatusAndDate() throws Exception {
        createDelivery(OWNER_A);
        long assigned = createAndAssign(OWNER_A, driver1);

        perform(get("/api/deliveries").param("status", "ASSIGNED"), ADMIN, null)
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(assigned));

        perform(get("/api/deliveries").param("status", "CREATED,ASSIGNED"), ADMIN, null)
                .andExpect(jsonPath("$", hasSize(2)));

        String today = LocalDate.now(java.time.ZoneId.of("Europe/Brussels")).toString();
        perform(get("/api/deliveries").param("from", today).param("to", today), OWNER_A, null)
                .andExpect(jsonPath("$", hasSize(2)));

        perform(get("/api/deliveries").param("from", "2000-01-01T00:00:00Z").param("to", "2000-01-02T00:00:00Z"), ADMIN, null)
                .andExpect(jsonPath("$", hasSize(0)));

        perform(get("/api/deliveries").param("from", "yesterday"), ADMIN, null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.from").exists());

        perform(get("/api/deliveries").param("status", "PENDING"), ADMIN, null)
                .andExpect(status().isBadRequest());
    }

    // ================================================================ helpers

    private long createDelivery(String ownerEmail) throws Exception {
        return json(perform(post("/api/deliveries"), ownerEmail, validBody())
                .andExpect(status().isCreated())).get("id").asLong();
    }

    private long createAndAssign(String ownerEmail, Driver driver) throws Exception {
        long id = createDelivery(ownerEmail);
        perform(put("/api/deliveries/{id}/assign", id), ADMIN, Map.of("driverId", driver.getId()))
                .andExpect(status().isOk());
        return id;
    }

    private Map<String, Object> validBody() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("customerName", "Awa Diop");
        body.put("customerPhone", "+32 470 12 34 56");
        body.put("deliveryAddress", "Rue de la Loi 16, 1000 Bruxelles");
        body.put("paymentMode", "PREPAID");
        return body;
    }

    private ResultActions perform(
            org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request,
            String email,
            Object body) throws Exception {

        request.header("Authorization", "Bearer " + token(email));
        if (body != null) {
            request.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body));
        }
        return mockMvc.perform(request);
    }

    private void assertAvailability(Driver driver, Driver.AvailabilityStatus expected) {
        assertThat(driverRepository.findById(driver.getId()).orElseThrow().getAvailabilityStatus())
                .isEqualTo(expected);
    }

    private String token(String email) throws Exception {
        String cached = tokens.get(email);
        if (cached != null) {
            return cached;
        }
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", PASSWORD))))
                .andExpect(status().isOk())
                .andReturn();
        String token = json(result).get("token").asText();
        tokens.put(email, token);
        return token;
    }

    private JsonNode json(ResultActions actions) throws Exception {
        return json(actions.andReturn());
    }

    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private Driver driver(User user) {
        return driverRepository.save(Driver.builder()
                .user(user)
                .phoneNumber("+32 400 00 00 00")
                .vehicleType("Scooter")
                .availabilityStatus(Driver.AvailabilityStatus.AVAILABLE)
                .build());
    }

    private User user(String email, User.Role role, Restaurant restaurant, boolean enabled) {
        return User.builder()
                .email(email)
                .password(passwordEncoder.encode(PASSWORD))
                .firstName("First")
                .lastName("Last")
                .role(role)
                .restaurant(restaurant)
                .enabled(enabled)
                .build();
    }
}
