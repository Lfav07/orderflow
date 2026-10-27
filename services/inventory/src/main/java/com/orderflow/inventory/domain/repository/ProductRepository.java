package com.orderflow.inventory.domain.repository;

import com.orderflow.inventory.domain.model.Product;
import com.orderflow.inventory.domain.model.ProductStatus;
import com.orderflow.inventory.domain.model.ProductUnit;

import java.util.List;
import java.util.UUID;

public interface ProductRepository {
    Product save(Product product);
    Product findById(UUID id);

    List<Product> findAll(int page, int size);

    List<Product> findByFilters(ProductStatus status, String name, ProductUnit unit, int page, int size);
}
