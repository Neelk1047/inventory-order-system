package com.neel.inventoryordersystem.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public class ProductBatchRequestDTO {

    @NotEmpty(message = "Product list cannot be empty")
    @Valid
    private List<ProductRequestDTO> products;

    public List<ProductRequestDTO> getProducts() {
        return products;
    }

    public void setProducts(List<ProductRequestDTO> products) {
        this.products = products;
    }
}