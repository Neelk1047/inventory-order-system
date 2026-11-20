package com.neel.inventoryordersystem;

import com.neel.inventoryordersystem.dto.OrderItemRequestDTO;
import com.neel.inventoryordersystem.dto.OrderRequestDTO;
import com.neel.inventoryordersystem.entity.Product;
import com.neel.inventoryordersystem.repository.OrderRepository;
import com.neel.inventoryordersystem.repository.ProductRepository;
import com.neel.inventoryordersystem.service.OrderService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

// Integration test: starts the full Spring context with a real (in-memory H2) database.
// Two threads try to buy the LAST item at the same time. Exactly one order may succeed.
// Without @Version on Product, both could succeed and the stock would be oversold.
@SpringBootTest
class OrderConcurrencyTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @AfterEach
    void cleanUp() {
        orderRepository.deleteAll();
        productRepository.deleteAll();
    }

    @Test
    void onlyOneOrderSucceedsWhenTwoCustomersBuyTheLastItem() throws Exception {
        Product product = productRepository.save(new Product("Last Item", new BigDecimal("100.00"), 1));
        Long productId = product.getId();

        int threadCount = 2;
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch go = new CountDownLatch(1);
        AtomicInteger succeeded = new AtomicInteger();
        AtomicInteger failed = new AtomicInteger();
        List<Future<Object>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            futures.add(pool.submit(() -> {
                ready.countDown();
                try {
                    go.await();                                   // start both threads together
                    orderService.placeOrder(orderFor(productId, 1));
                    succeeded.incrementAndGet();
                } catch (Exception e) {                           // out of stock OR optimistic lock conflict
                    failed.incrementAndGet();
                }
                return null;
            }));
        }

        ready.await();
        go.countDown();
        for (Future<Object> future : futures) {
            future.get();
        }
        pool.shutdown();

        int finalStock = productRepository.findById(productId).orElseThrow().getStockQuantity();
        assertEquals(1, succeeded.get(), "exactly one order should succeed");
        assertEquals(1, failed.get(), "the other order should be rejected");
        assertEquals(0, finalStock, "stock must end at 0, never negative");
    }

    private OrderRequestDTO orderFor(Long productId, int quantity) {
        OrderItemRequestDTO item = new OrderItemRequestDTO();
        item.setProductId(productId);
        item.setQuantity(quantity);
        OrderRequestDTO request = new OrderRequestDTO();
        request.setItems(List.of(item));
        return request;
    }
}
