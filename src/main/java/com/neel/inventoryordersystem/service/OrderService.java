package com.neel.inventoryordersystem.service;

import com.neel.inventoryordersystem.dto.OrderItemRequestDTO;
import com.neel.inventoryordersystem.dto.OrderRequestDTO;
import com.neel.inventoryordersystem.dto.OrderResponseDTO;
import com.neel.inventoryordersystem.entity.Order;
import com.neel.inventoryordersystem.entity.OrderItem;
import com.neel.inventoryordersystem.entity.Product;
import com.neel.inventoryordersystem.exception.InsufficientStockException;
import com.neel.inventoryordersystem.exception.OrderNotFoundException;
import com.neel.inventoryordersystem.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductService productService;

    public OrderService(OrderRepository orderRepository, ProductService productService) {
        this.orderRepository = orderRepository;
        this.productService = productService;
    }

    @Transactional
    public OrderResponseDTO placeOrder(OrderRequestDTO dto) {
        Order order = new Order();
        BigDecimal total = BigDecimal.ZERO;

        for (OrderItemRequestDTO itemDto : dto.getItems()) {
            Product product = productService.getProductEntityById(itemDto.getProductId());

            if (product.getStockQuantity() < itemDto.getQuantity()) {
                throw new InsufficientStockException(
                        "Insufficient stock for product '" + product.getName() +
                        "'. Available: " + product.getStockQuantity() +
                        ", Requested: " + itemDto.getQuantity());
            }

            product.setStockQuantity(product.getStockQuantity() - itemDto.getQuantity());

            BigDecimal unitPrice = product.getPrice();
            OrderItem orderItem = new OrderItem(product, itemDto.getQuantity(), unitPrice);
            order.addItem(orderItem);

            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(itemDto.getQuantity()));
            total = total.add(lineTotal);
        }

        order.setTotalAmount(total);
        Order saved = orderRepository.save(order);
        return OrderResponseDTO.from(saved);
    }

    public List<OrderResponseDTO> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(OrderResponseDTO::from)
                .toList();
    }

    public OrderResponseDTO getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException("Order with ID " + id + " not found"));
        return OrderResponseDTO.from(order);
    }
}

