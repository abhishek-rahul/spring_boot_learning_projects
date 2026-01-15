package com.flashkart.order.infra;

import com.flashkart.order.domain.InventoryReservation;
import com.flashkart.order.domain.InventoryReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface InventoryReservationRepository extends JpaRepository<InventoryReservation, UUID> {
    
    @Query("SELECT ir FROM InventoryReservation ir WHERE ir.orderId = :orderId AND ir.deletedAt IS NULL")
    List<InventoryReservation> findByOrderId(@Param("orderId") UUID orderId);
    
    @Query("SELECT ir FROM InventoryReservation ir WHERE ir.skuId = :skuId AND ir.status = :status AND ir.deletedAt IS NULL")
    List<InventoryReservation> findBySkuIdAndStatus(@Param("skuId") UUID skuId, @Param("status") InventoryReservationStatus status);
    
    @Query("SELECT ir FROM InventoryReservation ir WHERE ir.status = 'RESERVED' AND ir.expiresAt < :now AND ir.deletedAt IS NULL")
    List<InventoryReservation> findExpiredReservations(@Param("now") Instant now);
    
    @Modifying
    @Query("UPDATE InventoryReservation ir SET ir.status = :newStatus WHERE ir.orderId = :orderId AND ir.status = :oldStatus AND ir.deletedAt IS NULL")
    int updateStatusByOrderId(@Param("orderId") UUID orderId, @Param("oldStatus") InventoryReservationStatus oldStatus, @Param("newStatus") InventoryReservationStatus newStatus);
}
