package com.neel.inventoryordersystem.dto;

import com.neel.inventoryordersystem.entity.Order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponseDTO(Long id, LocalDateTime orderDate, BigDecimal totalAmount,
                                List<OrderItemResponseDTO> items) {

    public static OrderResponseDTO from(Order order) {
        List<OrderItemResponseDTO> itemDtos = order.getItems().stream()
                .map(OrderItemResponseDTO::from)
                .toList();
        return new OrderResponseDTO(order.getId(), order.getOrderDate(), order.getTotalAmount(), itemDtos);
    }
}
