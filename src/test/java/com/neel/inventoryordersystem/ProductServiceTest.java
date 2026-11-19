package com.neel.inventoryordersystem;

import com.neel.inventoryordersystem.dto.ProductRequestDTO;
import com.neel.inventoryordersystem.dto.ProductResponseDTO;
import com.neel.inventoryordersystem.entity.Product;
import com.neel.inventoryordersystem.exception.ProductInUseException;
import com.neel.inventoryordersystem.exception.ProductNotFoundException;
import com.neel.inventoryordersystem.repository.OrderItemRepository;
import com.neel.inventoryordersystem.repository.ProductRepository;
import com.neel.inventoryordersystem.service.ProductService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

// This is a unit test -- it does NOT need a real database.
// It uses Mockito to fake ("mock") the repository layer, so we can
// test the Service's logic in isolation.
@ExtendWith(MockitoExtension.class)
public class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    void shouldReturnProductWhenIdExists() {
        Product product = new Product("Laptop", new BigDecimal("55000.00"), 10);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        ProductResponseDTO result = productService.getProductById(1L);

        assertEquals("Laptop", result.name());
        verify(productRepository, times(1)).findById(1L);
    }

    @Test
    void shouldThrowExceptionWhenProductNotFound() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class, () -> {
            productService.getProductById(99L);
        });
    }

    @Test
    void shouldSaveNewProduct() {
        ProductRequestDTO dto = new ProductRequestDTO();
        dto.setName("Keyboard");
        dto.setPrice(new BigDecimal("1200.00"));
        dto.setStockQuantity(20);

        Product savedProduct = new Product("Keyboard", new BigDecimal("1200.00"), 20);
        when(productRepository.save(any(Product.class))).thenReturn(savedProduct);

        ProductResponseDTO result = productService.addProduct(dto);

        assertEquals("Keyboard", result.name());
        assertEquals(20, result.stockQuantity());
    }

    @Test
    void shouldUpdateExistingProduct() {
        Product product = new Product("Keyboard", new BigDecimal("20.00"), 5);
        ProductRequestDTO update = new ProductRequestDTO();
        update.setName("Mechanical Keyboard");
        update.setPrice(new BigDecimal("35.50"));
        update.setStockQuantity(8);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        ProductResponseDTO result = productService.updateProduct(1L, update);

        assertEquals("Mechanical Keyboard", result.name());
        assertEquals(new BigDecimal("35.50"), result.price());
        assertEquals(8, result.stockQuantity());
    }

    @Test
    void shouldDeleteExistingProduct() {
        Product product = new Product("Keyboard", BigDecimal.TEN, 20);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        productService.deleteProduct(1L);

        verify(productRepository).delete(product);
    }

    @Test
    void shouldNotDeleteProductThatIsPartOfExistingOrders() {
        Product product = new Product("Keyboard", BigDecimal.TEN, 20);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(orderItemRepository.existsByProductId(1L)).thenReturn(true);

        assertThrows(ProductInUseException.class, () -> productService.deleteProduct(1L));
        verify(productRepository, never()).delete(any());
    }
}
