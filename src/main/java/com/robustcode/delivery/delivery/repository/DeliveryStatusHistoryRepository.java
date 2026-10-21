package com.robustcode.delivery.delivery.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.robustcode.delivery.delivery.domain.DeliveryStatusHistory;

public interface DeliveryStatusHistoryRepository extends JpaRepository<DeliveryStatusHistory, Long> {

    List<DeliveryStatusHistory> findByDeliveryIdOrderByCreatedAtAscIdAsc(Long deliveryId);
}
