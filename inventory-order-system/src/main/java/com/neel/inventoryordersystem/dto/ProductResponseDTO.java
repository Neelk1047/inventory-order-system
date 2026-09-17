package com.neel.inventoryordersystem.dto;

import com.neel.inventoryordersystem.entity.Product;

import java.math.BigDecimal;

// A "record" is a shorthand Java class made just to hold data --
// it auto-generates the constructor, getters, equals/hashCode for you.
public record ProductResponseDTO(Long id, String name, BigDecimal price, int stockQuantity) {

    public static ProductResponseDTO from(Product product) {
        return new ProductResponseDTO(
                product.getId(),
                product.getName(),
                product.getPrice(),
                product.getStockQuantity()
        );
    }
}
