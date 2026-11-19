package com.neel.inventoryordersystem.exception;

// Thrown when someone tries to delete a product that already appears in past orders.
public class ProductInUseException extends RuntimeException {
    public ProductInUseException(String message) {
        super(message);
    }
}
