package com.flashkart.order.infra;

import com.flashkart.order.domain.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {
    
    @Query("SELECT oi FROM OrderItem oi WHERE oi.order.id = :orderId AND oi.deletedAt IS NULL")
    List<OrderItem> findByOrderId(@Param("orderId") UUID orderId);
}
