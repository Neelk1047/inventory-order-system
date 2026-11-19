package com.neel.inventoryordersystem.service;

import com.neel.inventoryordersystem.dto.ProductRequestDTO;
import com.neel.inventoryordersystem.dto.ProductResponseDTO;
import com.neel.inventoryordersystem.dto.ProductBatchRequestDTO;
import com.neel.inventoryordersystem.entity.Product;
import com.neel.inventoryordersystem.exception.ProductInUseException;
import com.neel.inventoryordersystem.exception.ProductNotFoundException;
import com.neel.inventoryordersystem.repository.OrderItemRepository;
import com.neel.inventoryordersystem.repository.ProductRepository;
import org.springframework.data.domain.Page;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final OrderItemRepository orderItemRepository;

    public ProductService(ProductRepository productRepository, OrderItemRepository orderItemRepository) {
        this.productRepository = productRepository;
        this.orderItemRepository = orderItemRepository;
    }

    public ProductResponseDTO addProduct(ProductRequestDTO dto) {
        Product product = new Product(dto.getName(), dto.getPrice(), dto.getStockQuantity());
        return ProductResponseDTO.from(productRepository.save(product));
    }

    public List<ProductResponseDTO> addProducts(ProductBatchRequestDTO batch) {
        List<Product> products = batch.getProducts().stream()
                .map(dto -> new Product(dto.getName(), dto.getPrice(), dto.getStockQuantity()))
                .toList();
        return productRepository.saveAll(products).stream()
                .map(ProductResponseDTO::from)
                .toList();
    }

    public Page<ProductResponseDTO> getProducts(String name, Pageable pageable) {
        Page<Product> page = (name == null || name.isBlank())
                ? productRepository.findAll(pageable)
                : productRepository.findByNameContainingIgnoreCase(name, pageable);
        return page.map(ProductResponseDTO::from);
    }

    public Product getProductEntityById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product with ID " + id + " not found"));
    }

    public ProductResponseDTO getProductById(Long id) {
        return ProductResponseDTO.from(getProductEntityById(id));
    }

    public ProductResponseDTO updateProduct(Long id, ProductRequestDTO dto) {
        Product product = getProductEntityById(id);
        product.setName(dto.getName());
        product.setPrice(dto.getPrice());
        product.setStockQuantity(dto.getStockQuantity());
        return ProductResponseDTO.from(productRepository.save(product));
    }

    public void deleteProduct(Long id) {
        Product product = getProductEntityById(id);
        // Past orders keep a reference to the product, so deleting it would break order history.
        if (orderItemRepository.existsByProductId(id)) {
            throw new ProductInUseException(
                    "Product with ID " + id + " is part of existing orders and cannot be deleted");
        }
        productRepository.delete(product);
    }
}

