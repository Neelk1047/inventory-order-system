package com.neel.inventoryordersystem.repository;

import com.neel.inventoryordersystem.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    // Derived query: Spring builds "SELECT COUNT(...) > 0 ... WHERE product_id = ?" from the method name.
    boolean existsByProductId(Long productId);
}
