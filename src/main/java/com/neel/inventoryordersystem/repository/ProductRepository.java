package com.neel.inventoryordersystem.repository;

import com.neel.inventoryordersystem.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    // Spring Data JPA reads this method NAME and builds the SQL query automatically --
    // no SQL written by us. "ContainingIgnoreCase" -> a case-insensitive LIKE '%name%'.
    Page<Product> findByNameContainingIgnoreCase(String name, Pageable pageable);
}

