package com.neel.inventoryordersystem.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public class ProductRequestDTO {

    @NotBlank(message = "Product name cannot be empty")
    private String name;

    @Positive(message = "Price must be greater than zero")
    private BigDecimal price;

    @Min(value = 0, message = "Stock quantity cannot be negative")
    private int stockQuantity;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public int getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(int stockQuantity) {
        this.stockQuantity = stockQuantity;
    }
}


