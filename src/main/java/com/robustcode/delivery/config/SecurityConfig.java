package com.robustcode.delivery.config;

import com.robustcode.delivery.security.JwtAuthenticationFilter;
import com.robustcode.delivery.security.RestAccessDeniedHandler;
import com.robustcode.delivery.security.RestAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
@RequiredArgsConstructor
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;

    @Value("${app.cors.allowed-origins:http://localhost:5173}")
    private String corsAllowedOrigins;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    // 1. Définition de la configuration CORS
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        List<String> allowedOrigins = Arrays.stream(corsAllowedOrigins.split(","))
            .map(String::trim)
            .filter(origin -> !origin.isEmpty())
            .collect(Collectors.toList());
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "Accept"));
        configuration.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .cors(cors -> cors.configurationSource(corsConfigurationSource())) // 2. Activation du CORS
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                // Deny by default: every endpoint must be listed explicitly.
                // Object-level ownership (own restaurant / assigned driver) is
                // enforced in the services through AccessGuard.
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/error").permitAll()

                        // Auth
                        .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/register").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/register-restaurant").hasRole("ADMIN")

                        // Actuator: only health and info are public
                        .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()

                        // Admin
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/users").hasRole("ADMIN")

                        // Restaurants
                        .requestMatchers(HttpMethod.GET, "/api/restaurants/me").hasRole("RESTAURANT")
                        .requestMatchers(HttpMethod.GET, "/api/restaurants").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/restaurants").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/restaurants/*/deliveries", "/api/restaurants/*/deliveries/status").hasAnyRole("ADMIN", "RESTAURANT")
                        .requestMatchers(HttpMethod.GET, "/api/restaurants/*").hasAnyRole("ADMIN", "RESTAURANT")

                        // Vehicles
                        .requestMatchers(HttpMethod.POST, "/api/vehicles").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/vehicles").hasAnyRole("ADMIN", "DISPATCHER")

                        // Drivers
                        .requestMatchers(HttpMethod.GET, "/api/drivers").hasAnyRole("ADMIN", "DISPATCHER")
                        .requestMatchers(HttpMethod.POST, "/api/drivers").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/drivers/me").hasRole("DRIVER")
                        .requestMatchers(HttpMethod.PATCH, "/api/drivers/me/location").hasRole("DRIVER")

                        // Deliveries
                        .requestMatchers(HttpMethod.GET, "/api/deliveries/me").hasRole("DRIVER")
                        .requestMatchers(HttpMethod.GET, "/api/deliveries", "/api/deliveries/*").hasAnyRole("ADMIN", "DISPATCHER", "RESTAURANT", "DRIVER")
                        .requestMatchers(HttpMethod.POST, "/api/deliveries").hasAnyRole("ADMIN", "DISPATCHER", "RESTAURANT")
                        .requestMatchers(HttpMethod.PUT, "/api/deliveries/*/assign").hasAnyRole("ADMIN", "DISPATCHER")
                        .requestMatchers(HttpMethod.PUT, "/api/deliveries/*/pickup", "/api/deliveries/*/start", "/api/deliveries/*/complete", "/api/deliveries/*/fail").hasRole("DRIVER")

                        // Orders
                        .requestMatchers(HttpMethod.POST, "/api/orders").hasRole("CUSTOMER")
                        .requestMatchers(HttpMethod.GET, "/api/orders/my-orders").hasRole("CUSTOMER")
                        .requestMatchers(HttpMethod.GET, "/api/orders/restaurant").hasRole("RESTAURANT")
                        .requestMatchers(HttpMethod.GET, "/api/orders/*/history").hasAnyRole("ADMIN", "RESTAURANT", "DRIVER")
                        .requestMatchers(HttpMethod.PATCH, "/api/orders/*/accept", "/api/orders/*/prepare", "/api/orders/*/ready").hasRole("RESTAURANT")
                        .requestMatchers(HttpMethod.PATCH, "/api/orders/*/pickup", "/api/orders/*/deliver").hasRole("DRIVER")
                        .requestMatchers(HttpMethod.PATCH, "/api/orders/*/assign-driver/*").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/orders").hasRole("ADMIN")

                        .anyRequest().denyAll()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}