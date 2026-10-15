package com.robustcode.delivery.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.robustcode.delivery.delivery.domain.Delivery;
import com.robustcode.delivery.delivery.repository.DeliveryRepository;
import com.robustcode.delivery.driver.domain.Driver;
import com.robustcode.delivery.driver.repository.DriverRepository;
import com.robustcode.delivery.order.domain.Order;
import com.robustcode.delivery.order.domain.OrderStatusHistory;
import com.robustcode.delivery.order.repository.OrderRepository;
import com.robustcode.delivery.order.repository.OrderStatusHistoryRepository;
import com.robustcode.delivery.restaurant.domain.Restaurant;
import com.robustcode.delivery.restaurant.repository.RestaurantRepository;
import com.robustcode.delivery.user.domain.User;
import com.robustcode.delivery.user.repository.UserRepository;
import com.robustcode.delivery.vehicle.domain.Vehicle;
import com.robustcode.delivery.vehicle.repository.VehicleRepository;

/**
 * End-to-end security checks through the real filter chain (real JWTs).
 */
@SpringBootTest
@AutoConfigureMockMvc
class SecurityIntegrationTest {

    private static final String PASSWORD = "correct-horse-battery";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private UserRepository userRepository;
    @Autowired private RestaurantRepository restaurantRepository;
    @Autowired private DriverRepository driverRepository;
    @Autowired private VehicleRepository vehicleRepository;
    @Autowired private DeliveryRepository deliveryRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private OrderStatusHistoryRepository historyRepository;

    private Restaurant restaurantA;
    private Restaurant restaurantB;
    private Delivery deliveryA;
    private Delivery deliveryB;
    private Order orderA;
    private Order orderB;

    @BeforeEach
    void setUp() {
        historyRepository.deleteAll();
        orderRepository.deleteAll();
        deliveryRepository.deleteAll();
        driverRepository.deleteAll();
        vehicleRepository.deleteAll();
        userRepository.deleteAll();
        restaurantRepository.deleteAll();

        restaurantA = restaurantRepository.save(restaurant("Resto A", "a@resto.test"));
        restaurantB = restaurantRepository.save(restaurant("Resto B", "b@resto.test"));

        userRepository.save(user("admin@rdp.test", User.Role.ADMIN, null));
        userRepository.save(user("dispatcher@rdp.test", User.Role.DISPATCHER, null));
        userRepository.save(user("owner-a@rdp.test", User.Role.RESTAURANT, restaurantA));
        userRepository.save(user("owner-b@rdp.test", User.Role.RESTAURANT, restaurantB));
        User customer = userRepository.save(user("customer@rdp.test", User.Role.CUSTOMER, null));
        User driverUser = userRepository.save(user("driver@rdp.test", User.Role.DRIVER, null));
        userRepository.save(user("other-driver@rdp.test", User.Role.DRIVER, null));

        Vehicle vehicle = vehicleRepository.save(Vehicle.builder()
                .vehicleType("Bike")
                .plateNumber("TEST-1")
                .status(Vehicle.Status.ASSIGNED)
                .build());

        Driver driver = driverRepository.save(Driver.builder()
                .user(driverUser)
                .vehicle(vehicle)
                .phoneNumber("+32 400 00 00 00")
                .availabilityStatus(Driver.AvailabilityStatus.AVAILABLE)
                .build());

        deliveryA = deliveryRepository.save(delivery(customer, restaurantA, driver));
        deliveryB = deliveryRepository.save(delivery(customer, restaurantB, null));

        orderA = orderRepository.save(order(customer, restaurantA, driver));
        orderB = orderRepository.save(order(customer, restaurantB, null));

        historyRepository.save(history(orderA));
        historyRepository.save(history(orderB));
    }

    // ---------------------------------------------------------------- auth

    @Test
    void publicRegistrationRejectsPrivilegedRoles() throws Exception {
        for (String role : new String[] {"ADMIN", "DISPATCHER", "RESTAURANT"}) {
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(registerBody("evil-" + role.toLowerCase() + "@rdp.test", role)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.error").value("Bad Request"))
                    .andExpect(jsonPath("$.path").value("/api/auth/register"))
                    .andExpect(jsonPath("$.timestamp").exists());

            assertThat(userRepository.existsByEmail("evil-" + role.toLowerCase() + "@rdp.test")).isFalse();
        }
    }

    @Test
    void driverRegistrationIsDisabledUntilAdminEnablesIt() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "new-driver@rdp.test",
                                "password", PASSWORD,
                                "firstName", "New",
                                "lastName", "Driver",
                                "phoneNumber", "+32 470 00 00 00",
                                "vehicleType", "Scooter",
                                "vehiclePlate", "NEW-1",
                                "role", "DRIVER"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("DRIVER"))
                .andExpect(jsonPath("$.enabled").value(false))
                .andReturn();

        long newDriverId = json(result).get("id").asLong();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("new-driver@rdp.test")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Account is disabled or pending administrator approval"));

        String adminToken = login("admin@rdp.test");

        mockMvc.perform(get("/api/admin/users")
                        .param("role", "DRIVER")
                        .param("enabled", "false")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].email").value("new-driver@rdp.test"));

        // Non-admin cannot enable accounts
        mockMvc.perform(patch("/api/admin/users/{id}/enable", newDriverId)
                        .header("Authorization", bearer(login("dispatcher@rdp.test"))))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/admin/users/{id}/enable", newDriverId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true));

        String driverToken = login("new-driver@rdp.test");
        assertThat(driverToken).isNotBlank();

        mockMvc.perform(patch("/api/admin/users/{id}/disable", newDriverId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));

        // Existing token of a disabled account no longer authenticates
        mockMvc.perform(get("/api/drivers/me").header("Authorization", bearer(driverToken)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void customerRegistrationIsEnabledImmediately() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody("new-customer@rdp.test", "CUSTOMER")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.enabled").value(true));

        assertThat(login("new-customer@rdp.test")).isNotBlank();
    }

    @Test
    void registrationValidationErrorsListFields() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-an-email\",\"password\":\"short\",\"role\":\"CUSTOMER\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.password").exists())
                .andExpect(jsonPath("$.fieldErrors.firstName").exists());
    }

    @Test
    void duplicateEmailIsConflict() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody("customer@rdp.test", "CUSTOMER")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Email already exists"));
    }

    @Test
    void wrongPasswordIsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@rdp.test\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void registerRestaurantIsAdminOnly() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "restaurantName", "New Resto",
                "phoneNumber", "+32 2 000 00 00",
                "restaurantEmail", "new@resto.test",
                "address", "Rue Neuve 1",
                "adminEmail", "owner-new@rdp.test",
                "adminPassword", PASSWORD,
                "adminFirstName", "Owner",
                "adminLastName", "New"));

        mockMvc.perform(post("/api/auth/register-restaurant")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/auth/register-restaurant")
                        .header("Authorization", bearer(login("owner-a@rdp.test")))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/auth/register-restaurant")
                        .header("Authorization", bearer(login("admin@rdp.test")))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("RESTAURANT"));
    }

    @Test
    void createUserIsAdminOnly() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "email", "created@rdp.test",
                "password", PASSWORD,
                "firstName", "Created",
                "lastName", "User",
                "role", "ADMIN"));

        mockMvc.perform(post("/api/users")
                        .header("Authorization", bearer(login("driver@rdp.test")))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/users")
                        .header("Authorization", bearer(login("admin@rdp.test")))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
    }

    // ---------------------------------------------------------------- JWT

    @Test
    void missingOrInvalidTokenIsUnauthorizedNotServerError() throws Exception {
        mockMvc.perform(get("/api/deliveries"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.path").value("/api/deliveries"));

        mockMvc.perform(get("/api/deliveries").header("Authorization", "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized());

        String[] parts = login("admin@rdp.test").split("\\.");
        String forgedPayload = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(
                "{\"sub\":\"admin@rdp.test\",\"role\":\"ADMIN\",\"exp\":9999999999}".getBytes());
        String tampered = parts[0] + "." + forgedPayload + "." + parts[2];
        mockMvc.perform(get("/api/deliveries").header("Authorization", bearer(tampered)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenForDeletedUserIsUnauthorized() throws Exception {
        String token = login("other-driver@rdp.test");
        userRepository.delete(userRepository.findByEmail("other-driver@rdp.test").orElseThrow());

        mockMvc.perform(get("/api/deliveries").header("Authorization", bearer(token)))
                .andExpect(status().isUnauthorized());
    }

    // ---------------------------------------------------------------- actuator / fallthrough

    @Test
    void onlyHealthAndInfoActuatorEndpointsArePublic() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
        mockMvc.perform(get("/actuator/env")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/actuator/env").header("Authorization", bearer(login("admin@rdp.test"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void unknownEndpointsAreDeniedByDefault() throws Exception {
        mockMvc.perform(get("/api/something-new").header("Authorization", bearer(login("admin@rdp.test"))))
                .andExpect(status().isForbidden());
    }

    // ---------------------------------------------------------------- restaurant scoping

    @Test
    void restaurantCannotReadAnotherRestaurantsDeliveries() throws Exception {
        String tokenA = login("owner-a@rdp.test");

        mockMvc.perform(get("/api/restaurants/{id}/deliveries", restaurantB.getId())
                        .header("Authorization", bearer(tokenA)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        mockMvc.perform(get("/api/restaurants/{id}/deliveries/status", restaurantB.getId())
                        .param("status", "PENDING")
                        .header("Authorization", bearer(tokenA)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/restaurants/{id}/deliveries", restaurantA.getId())
                        .header("Authorization", bearer(tokenA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(deliveryA.getId()));

        mockMvc.perform(get("/api/restaurants/{id}", restaurantB.getId())
                        .header("Authorization", bearer(tokenA)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/restaurants/{id}/deliveries", restaurantB.getId())
                        .header("Authorization", bearer(login("admin@rdp.test"))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/restaurants/{id}/deliveries", restaurantA.getId())
                        .header("Authorization", bearer(login("customer@rdp.test"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void restaurantCannotReadAnotherRestaurantsOrderHistory() throws Exception {
        String tokenA = login("owner-a@rdp.test");

        mockMvc.perform(get("/api/orders/{id}/history", orderB.getId())
                        .header("Authorization", bearer(tokenA)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/orders/{id}/history", orderA.getId())
                        .header("Authorization", bearer(tokenA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        mockMvc.perform(get("/api/orders/{id}/history", orderB.getId())
                        .header("Authorization", bearer(login("admin@rdp.test"))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/orders/{id}/history", 999_999L)
                        .header("Authorization", bearer(login("admin@rdp.test"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Order not found"));
    }

    @Test
    void driverSeesOnlyAssignedOrderHistory() throws Exception {
        mockMvc.perform(get("/api/orders/{id}/history", orderA.getId())
                        .header("Authorization", bearer(login("driver@rdp.test"))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/orders/{id}/history", orderB.getId())
                        .header("Authorization", bearer(login("driver@rdp.test"))))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/orders/{id}/history", orderA.getId())
                        .header("Authorization", bearer(login("other-driver@rdp.test"))))
                .andExpect(status().isForbidden());
    }

    // ---------------------------------------------------------------- /api/deliveries scoping

    @Test
    void deliveriesListIsScopedByRole() throws Exception {
        mockMvc.perform(get("/api/deliveries").header("Authorization", bearer(login("admin@rdp.test"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        mockMvc.perform(get("/api/deliveries").header("Authorization", bearer(login("dispatcher@rdp.test"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        mockMvc.perform(get("/api/deliveries").header("Authorization", bearer(login("owner-b@rdp.test"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(deliveryB.getId()));

        mockMvc.perform(get("/api/deliveries").header("Authorization", bearer(login("driver@rdp.test"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(deliveryA.getId()));

        mockMvc.perform(get("/api/deliveries").header("Authorization", bearer(login("other-driver@rdp.test"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        mockMvc.perform(get("/api/deliveries").header("Authorization", bearer(login("customer@rdp.test"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void deliveryByIdIsScoped() throws Exception {
        mockMvc.perform(get("/api/deliveries/{id}", deliveryB.getId())
                        .header("Authorization", bearer(login("owner-a@rdp.test"))))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/deliveries/{id}", deliveryA.getId())
                        .header("Authorization", bearer(login("owner-a@rdp.test"))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/deliveries/{id}", deliveryB.getId())
                        .header("Authorization", bearer(login("driver@rdp.test"))))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/deliveries/{id}", deliveryB.getId())
                        .header("Authorization", bearer(login("dispatcher@rdp.test"))))
                .andExpect(status().isOk());
    }

    @Test
    void restaurantCanCreateDeliveriesOnlyForItself() throws Exception {
        long customerId = userRepository.findByEmail("customer@rdp.test").orElseThrow().getId();
        String tokenA = login("owner-a@rdp.test");

        mockMvc.perform(post("/api/deliveries")
                        .header("Authorization", bearer(tokenA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deliveryBody(customerId, restaurantB.getId())))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/deliveries")
                        .header("Authorization", bearer(tokenA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deliveryBody(customerId, restaurantA.getId())))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/deliveries")
                        .header("Authorization", bearer(login("driver@rdp.test")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deliveryBody(customerId, restaurantA.getId())))
                .andExpect(status().isForbidden());
    }

    @Test
    void onlyAdminOrDispatcherAssignsDrivers() throws Exception {
        long driverId = driverRepository.findAll().get(0).getId();

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put("/api/deliveries/{id}/assign", deliveryB.getId())
                        .header("Authorization", bearer(login("owner-b@rdp.test")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"driverId\":" + driverId + "}"))
                .andExpect(status().isForbidden());
    }

    // ---------------------------------------------------------------- helpers

    private String login(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(email)))
                .andExpect(status().isOk())
                .andReturn();
        return json(result).get("token").asText();
    }

    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private String loginBody(String email) throws Exception {
        return objectMapper.writeValueAsString(Map.of("email", email, "password", PASSWORD));
    }

    private String registerBody(String email, String role) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "email", email,
                "password", PASSWORD,
                "firstName", "First",
                "lastName", "Last",
                "role", role));
    }

    private String deliveryBody(long customerId, long restaurantId) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "customerId", customerId,
                "restaurantId", restaurantId,
                "pickupAddress", "Pickup 1",
                "deliveryAddress", "Dropoff 1"));
    }

    private User user(String email, User.Role role, Restaurant restaurant) {
        return User.builder()
                .email(email)
                .password(passwordEncoder.encode(PASSWORD))
                .firstName("First")
                .lastName("Last")
                .role(role)
                .restaurant(restaurant)
                .enabled(true)
                .build();
    }

    private static Restaurant restaurant(String name, String email) {
        return Restaurant.builder()
                .name(name)
                .email(email)
                .address("Rue de Test 1")
                .status(Restaurant.Status.ACTIVE)
                .build();
    }

    private static Delivery delivery(User customer, Restaurant restaurant, Driver driver) {
        return Delivery.builder()
                .customer(customer)
                .restaurant(restaurant)
                .driver(driver)
                .status(driver == null ? Delivery.Status.PENDING : Delivery.Status.ASSIGNED)
                .pickupAddress("Pickup")
                .deliveryAddress("Dropoff")
                .build();
    }

    private static Order order(User customer, Restaurant restaurant, Driver driver) {
        return Order.builder()
                .customer(customer)
                .restaurant(restaurant)
                .driver(driver)
                .status(Order.Status.PENDING)
                .totalPrice(new BigDecimal("12.50"))
                .deliveryAddress("Dropoff")
                .build();
    }

    private static OrderStatusHistory history(Order order) {
        return OrderStatusHistory.builder()
                .order(order)
                .newStatus(Order.Status.PENDING)
                .changedBy("test")
                .build();
    }
}
