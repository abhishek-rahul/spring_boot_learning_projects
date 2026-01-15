package com.flashkart.order.service;

import com.flashkart.catalog.domain.Sku;
import com.flashkart.catalog.infra.SkuRepository;
import com.flashkart.order.domain.InventoryReservation;
import com.flashkart.order.domain.InventoryReservationStatus;
import com.flashkart.order.infra.InventoryReservationRepository;
import com.flashkart.shared.error.BusinessException;
import com.flashkart.shared.error.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing inventory reservations during checkout.
 * Uses pessimistic locking to prevent overselling in high concurrency scenarios.
 */
@Service
public class InventoryReservationService {

    private static final Logger logger = LoggerFactory.getLogger(InventoryReservationService.class);
    private static final int RESERVATION_TTL_MINUTES = 15; // Reservations expire after 15 minutes

    private final SkuRepository skuRepository;
    private final InventoryReservationRepository inventoryReservationRepository;

    public InventoryReservationService(
            SkuRepository skuRepository,
            InventoryReservationRepository inventoryReservationRepository) {
        this.skuRepository = skuRepository;
        this.inventoryReservationRepository = inventoryReservationRepository;
    }

    /**
     * Reserve inventory for an order.
     * Uses READ COMMITTED isolation with pessimistic locking to prevent overselling.
     * 
     * @param orderId The order ID
     * @param skuQuantities Map of SKU ID to quantity to reserve
     * @return List of created inventory reservations
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public List<InventoryReservation> reserveInventory(UUID orderId, Map<UUID, Integer> skuQuantities) {
        Instant expiresAt = Instant.now().plus(RESERVATION_TTL_MINUTES, ChronoUnit.MINUTES);
        List<InventoryReservation> reservations = skuQuantities.entrySet().stream()
                .map(entry -> {
                    UUID skuId = entry.getKey();
                    Integer quantity = entry.getValue();
                    
                    // Use pessimistic lock to prevent concurrent modifications
                    Sku sku = skuRepository.findByIdForUpdate(skuId)
                            .orElseThrow(() -> new BusinessException(
                                    ErrorCode.SKU_NOT_FOUND,
                                    "SKU not found: " + skuId,
                                    false
                            ));

                    // Check availability
                    int availableQuantity = sku.getAvailableQuantity();
                    if (quantity > availableQuantity) {
                        throw new BusinessException(
                                ErrorCode.INSUFFICIENT_STOCK,
                                String.format("Insufficient stock for SKU %s. Available: %d, Requested: %d", 
                                        skuId, availableQuantity, quantity),
                                false
                        );
                    }

                    // Reserve stock
                    sku.setReservedQuantity(sku.getReservedQuantity() + quantity);
                    skuRepository.save(sku);

                    // Create reservation record
                    InventoryReservation reservation = new InventoryReservation(
                            orderId, skuId, quantity, expiresAt
                    );
                    return inventoryReservationRepository.save(reservation);
                })
                .collect(Collectors.toList());

        logger.info("Reserved inventory for order: orderId={}, reservations={}", orderId, reservations.size());
        return reservations;
    }

    /**
     * Confirm inventory reservations (when payment succeeds).
     * Moves reservations from RESERVED to CONFIRMED status.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void confirmReservations(UUID orderId) {
        int updated = inventoryReservationRepository.updateStatusByOrderId(
                orderId,
                InventoryReservationStatus.RESERVED,
                InventoryReservationStatus.CONFIRMED
        );
        logger.info("Confirmed inventory reservations for order: orderId={}, count={}", orderId, updated);
    }

    /**
     * Release inventory reservations (when order fails or is cancelled).
     * Releases reserved stock back to available inventory.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void releaseReservations(UUID orderId) {
        List<InventoryReservation> reservations = inventoryReservationRepository.findByOrderId(orderId);
        
        for (InventoryReservation reservation : reservations) {
            if (reservation.getStatus() == InventoryReservationStatus.RESERVED) {
                // Release stock back to available
                Sku sku = skuRepository.findById(reservation.getSkuId())
                        .orElse(null);
                
                if (sku != null) {
                    int currentReserved = sku.getReservedQuantity();
                    sku.setReservedQuantity(Math.max(0, currentReserved - reservation.getQuantity()));
                    skuRepository.save(sku);
                }

                reservation.setStatus(InventoryReservationStatus.RELEASED);
                inventoryReservationRepository.save(reservation);
            }
        }

        logger.info("Released inventory reservations for order: orderId={}, count={}", orderId, reservations.size());
    }
}
