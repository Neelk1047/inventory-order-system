package com.neel.inventoryordersystem;

import com.neel.inventoryordersystem.dto.OrderItemRequestDTO;
import com.neel.inventoryordersystem.dto.OrderRequestDTO;
import com.neel.inventoryordersystem.dto.OrderResponseDTO;
import com.neel.inventoryordersystem.entity.Order;
import com.neel.inventoryordersystem.entity.Product;
import com.neel.inventoryordersystem.exception.InsufficientStockException;
import com.neel.inventoryordersystem.repository.OrderRepository;
import com.neel.inventoryordersystem.service.OrderService;
import com.neel.inventoryordersystem.service.ProductService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductService productService;

    @InjectMocks
    private OrderService orderService;

    @Test
    void shouldReduceStockAndCalculateTotalCorrectly() {
        Product mouse = new Product("Mouse", new BigDecimal("500.00"), 10);
        mouse.setId(1L);
        when(productService.getProductEntityById(1L)).thenReturn(mouse);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderRequestDTO request = new OrderRequestDTO();
        OrderItemRequestDTO item = new OrderItemRequestDTO();
        item.setProductId(1L);
        item.setQuantity(3);
        request.setItems(List.of(item));

        OrderResponseDTO result = orderService.placeOrder(request);

        assertEquals(new BigDecimal("1500.00"), result.totalAmount());
        assertEquals(7, mouse.getStockQuantity()); // 10 - 3
    }

    @Test
    void shouldThrowWhenStockIsInsufficient() {
        Product mouse = new Product("Mouse", new BigDecimal("500.00"), 2);
        when(productService.getProductEntityById(1L)).thenReturn(mouse);

        OrderRequestDTO request = new OrderRequestDTO();
        OrderItemRequestDTO item = new OrderItemRequestDTO();
        item.setProductId(1L);
        item.setQuantity(5); // more than the 2 in stock
        request.setItems(List.of(item));

        assertThrows(InsufficientStockException.class, () -> orderService.placeOrder(request));
        verify(orderRepository, never()).save(any()); // nothing should be saved on failure
    }
}
