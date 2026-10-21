package com.robustcode.delivery.delivery.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.robustcode.delivery.delivery.domain.Delivery;
import com.robustcode.delivery.delivery.dto.AssignDriverRequest;
import com.robustcode.delivery.delivery.dto.CancelDeliveryRequest;
import com.robustcode.delivery.delivery.dto.CreateDeliveryRequest;
import com.robustcode.delivery.delivery.dto.DeliveryHistoryResponse;
import com.robustcode.delivery.delivery.dto.DeliveryResponse;
import com.robustcode.delivery.delivery.dto.FailDeliveryRequest;
import com.robustcode.delivery.delivery.service.DeliveryService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Delivery API (contract: DeliveryPlatform/docs/api-contract-v2.md).
 * Role rules are in SecurityConfig (and repeated here); ownership rules are
 * enforced in {@link DeliveryService}.
 */
@RestController
@RequestMapping("/api/deliveries")
@RequiredArgsConstructor
public class DeliveryController {

    private final DeliveryService service;

    @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER','RESTAURANT','DRIVER')")
    @GetMapping
    public List<DeliveryResponse> search(
            @RequestParam(required = false) List<Delivery.Status> status,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            Authentication authentication) {

        return service.search(authentication.getName(), status, from, to);
    }

    @PreAuthorize("hasRole('DRIVER')")
    @GetMapping("/me")
    public List<DeliveryResponse> getMyDeliveries(Authentication authentication) {
        return service.findMyDeliveries(authentication.getName());
    }

    @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER','RESTAURANT','DRIVER')")
    @GetMapping("/{id}")
    public DeliveryResponse getById(@PathVariable Long id, Authentication authentication) {
        return service.getById(id, authentication.getName());
    }

    @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER','RESTAURANT','DRIVER')")
    @GetMapping("/{id}/history")
    public List<DeliveryHistoryResponse> getHistory(@PathVariable Long id, Authentication authentication) {
        return service.getHistory(id, authentication.getName());
    }

    @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER','RESTAURANT')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DeliveryResponse create(
            @Valid @RequestBody CreateDeliveryRequest request,
            Authentication authentication) {

        return service.create(request, authentication.getName());
    }

    @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER')")
    @PutMapping("/{id}/assign")
    public DeliveryResponse assignDriver(
            @PathVariable Long id,
            @Valid @RequestBody AssignDriverRequest request,
            Authentication authentication) {

        return service.assignDriver(id, request.getDriverId(), authentication.getName());
    }

    @PreAuthorize("hasAnyRole('ADMIN','RESTAURANT')")
    @PatchMapping("/{id}/ready")
    public DeliveryResponse markReady(@PathVariable Long id, Authentication authentication) {
        return service.markReady(id, authentication.getName());
    }

    @PreAuthorize("hasAnyRole('ADMIN','RESTAURANT')")
    @PatchMapping("/{id}/cancel")
    public DeliveryResponse cancel(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) CancelDeliveryRequest request,
            Authentication authentication) {

        return service.cancel(id, request == null ? null : request.reason(), authentication.getName());
    }

    // ---------------------------------------------------------------- driver (mobile v1 paths)

    @PreAuthorize("hasRole('DRIVER')")
    @PutMapping("/{id}/pickup")
    public DeliveryResponse pickupDelivery(@PathVariable Long id, Authentication authentication) {
        return service.pickupDelivery(id, authentication.getName());
    }

    @PreAuthorize("hasRole('DRIVER')")
    @PutMapping("/{id}/start")
    public DeliveryResponse startDelivery(@PathVariable Long id, Authentication authentication) {
        return service.startDelivery(id, authentication.getName());
    }

    @PreAuthorize("hasRole('DRIVER')")
    @PutMapping("/{id}/complete")
    public DeliveryResponse completeDelivery(@PathVariable Long id, Authentication authentication) {
        return service.completeDelivery(id, authentication.getName());
    }

    @PreAuthorize("hasRole('DRIVER')")
    @PutMapping("/{id}/fail")
    public DeliveryResponse failDelivery(
            @PathVariable Long id,
            @Valid @RequestBody FailDeliveryRequest request,
            Authentication authentication) {

        return service.failDelivery(id, authentication.getName(), request.getReason());
    }
}
