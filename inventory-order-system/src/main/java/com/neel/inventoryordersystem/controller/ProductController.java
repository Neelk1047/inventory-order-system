package com.neel.inventoryordersystem.controller;

import com.neel.inventoryordersystem.dto.ProductRequestDTO;
import com.neel.inventoryordersystem.dto.ProductResponseDTO;
import com.neel.inventoryordersystem.dto.ProductBatchRequestDTO;
import com.neel.inventoryordersystem.service.ProductService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    public ResponseEntity<ProductResponseDTO> addProduct(@Valid @RequestBody ProductRequestDTO dto) {
        return new ResponseEntity<>(productService.addProduct(dto), HttpStatus.CREATED);
    }

    @PostMapping("/batch")
    public ResponseEntity<List<ProductResponseDTO>> addProducts(@Valid @RequestBody ProductBatchRequestDTO batch) {
        return new ResponseEntity<>(productService.addProducts(batch), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<Page<ProductResponseDTO>> getProducts(
            @RequestParam(required = false) String name,
            Pageable pageable) {
        return ResponseEntity.ok(productService.getProducts(name, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponseDTO> getProductById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductResponseDTO> updateProduct(@PathVariable Long id,
                                                              @Valid @RequestBody ProductRequestDTO dto) {
        return ResponseEntity.ok(productService.updateProduct(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}

