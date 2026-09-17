package com.neel.inventoryordersystem.dto;

import com.neel.inventoryordersystem.entity.OrderItem;

import java.math.BigDecimal;

public record OrderItemResponseDTO(Long productId, String productName, int quantity, BigDecimal unitPrice) {

    public static OrderItemResponseDTO from(OrderItem item) {
        return new OrderItemResponseDTO(
                item.getProduct().getId(),
                item.getProduct().getName(),
                item.getQuantity(),
                item.getUnitPrice()
        );
    }
}
