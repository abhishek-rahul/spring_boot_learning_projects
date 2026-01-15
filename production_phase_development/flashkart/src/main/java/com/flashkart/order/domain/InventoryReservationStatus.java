package com.flashkart.order.domain;

/**
 * Inventory reservation status enumeration.
 */
public enum InventoryReservationStatus {
    RESERVED,   // Stock reserved for order
    CONFIRMED,  // Reservation confirmed (order paid)
    RELEASED,   // Reservation released (order cancelled/failed)
    EXPIRED     // Reservation expired
}
