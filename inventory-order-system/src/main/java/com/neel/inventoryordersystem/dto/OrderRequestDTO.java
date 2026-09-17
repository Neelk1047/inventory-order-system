package com.neel.inventoryordersystem.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public class OrderRequestDTO {

    @NotEmpty(message = "An order must contain at least one item")
    @Valid
    private List<OrderItemRequestDTO> items;

    public List<OrderItemRequestDTO> getItems() {
        return items;
    }

    public void setItems(List<OrderItemRequestDTO> items) {
        this.items = items;
    }
}

