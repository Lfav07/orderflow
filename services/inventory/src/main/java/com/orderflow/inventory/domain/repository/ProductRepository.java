package com.orderflow.inventory.domain.repository;

import com.orderflow.inventory.domain.model.Product;

public interface ProductRepository {
    Product save(Product product);
}
