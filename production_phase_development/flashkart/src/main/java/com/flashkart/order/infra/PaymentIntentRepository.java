package com.flashkart.order.infra;

import com.flashkart.order.domain.PaymentIntent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentIntentRepository extends JpaRepository<PaymentIntent, UUID> {
    
    @Query("SELECT pi FROM PaymentIntent pi WHERE pi.order.id = :orderId AND pi.deletedAt IS NULL")
    Optional<PaymentIntent> findByOrderId(@Param("orderId") UUID orderId);
}
