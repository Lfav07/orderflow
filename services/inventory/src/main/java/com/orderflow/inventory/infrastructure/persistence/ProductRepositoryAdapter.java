package com.orderflow.inventory.infrastructure.persistence;

import com.orderflow.inventory.domain.model.Product;
import com.orderflow.inventory.domain.model.ProductStatus;
import com.orderflow.inventory.domain.model.ProductUnit;
import com.orderflow.inventory.domain.repository.ProductRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class ProductRepositoryAdapter implements ProductRepository {
    private final ProductJpaRepository productJpaRepository;

    public ProductRepositoryAdapter(ProductJpaRepository productJpaRepository) {
        this.productJpaRepository = productJpaRepository;
    }
    @Override
    public Product save(Product product) {
        ProductJpaEntity jpaEntity = ProductJpaMapper.toProductJpaEntity(product);
        ProductJpaEntity savedEntity = productJpaRepository.save(jpaEntity);
        return ProductJpaMapper.toDomainEntity(savedEntity);
    }

    @Override
    public Product findById(UUID id) {
        return productJpaRepository.findById(id)
                .map(ProductJpaMapper::toDomainEntity)
                .orElse(null);
    }

    @Override
    public List<Product> findAll(int page, int size) {
        return productJpaRepository.findAll(PageRequest.of(page, size, Sort.by("name").ascending()))
                .map(ProductJpaMapper::toDomainEntity)
                .getContent();
    }

    @Override
    public List<Product> findByFilters(ProductStatus status, String name, ProductUnit unit, int page, int size) {
        return productJpaRepository.findByFilters(status, unit, name,
                        PageRequest.of(page, size, Sort.by("name").ascending()))
                .map(ProductJpaMapper::toDomainEntity)
                .getContent();
    }
}
