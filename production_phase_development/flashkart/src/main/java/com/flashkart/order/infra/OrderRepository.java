package com.flashkart.order.infra;

import com.flashkart.order.domain.Order;
import com.flashkart.order.domain.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {
    
    Optional<Order> findByOrderNumber(String orderNumber);
    
    @Query("SELECT o FROM Order o WHERE o.id = :id AND o.userId = :userId AND o.deletedAt IS NULL")
    Optional<Order> findByIdAndUserId(@Param("id") UUID id, @Param("userId") UUID userId);
    
    @Query("SELECT o FROM Order o WHERE o.userId = :userId AND o.status = :status AND o.deletedAt IS NULL")
    java.util.List<Order> findByUserIdAndStatus(@Param("userId") UUID userId, @Param("status") OrderStatus status);
}
