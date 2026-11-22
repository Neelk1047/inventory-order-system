package com.neel.inventoryordersystem.controller;

import com.neel.inventoryordersystem.dto.ProductResponseDTO;
import com.neel.inventoryordersystem.exception.ProductInUseException;
import com.neel.inventoryordersystem.exception.ProductNotFoundException;
import com.neel.inventoryordersystem.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// @WebMvcTest starts ONLY the web layer (controllers + exception handler + JSON conversion).
// The service is replaced by a Mockito mock, so no database is needed.
@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    @Test
    void createProduct_withValidBody_returns201() throws Exception {
        when(productService.addProduct(any()))
                .thenReturn(new ProductResponseDTO(1L, "Laptop", new BigDecimal("55000.00"), 10));

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Laptop\",\"price\":55000.00,\"stockQuantity\":10}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Laptop"));
    }

    @Test
    void createProduct_withBlankName_returns400WithFieldError() throws Exception {
        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"price\":100.00,\"stockQuantity\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.name").value("Product name cannot be empty"));
    }

    @Test
    void createProduct_withMalformedJson_returns400() throws Exception {
        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ this is not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void getProduct_whenMissing_returns404() throws Exception {
        when(productService.getProductById(99L))
                .thenThrow(new ProductNotFoundException("Product with ID 99 not found"));

        mockMvc.perform(get("/products/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Product with ID 99 not found"));
    }

    @Test
    void deleteProduct_whenUsedInOrders_returns409() throws Exception {
        doThrow(new ProductInUseException("Product with ID 1 is part of existing orders and cannot be deleted"))
                .when(productService).deleteProduct(1L);

        mockMvc.perform(delete("/products/1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").exists());
    }
}