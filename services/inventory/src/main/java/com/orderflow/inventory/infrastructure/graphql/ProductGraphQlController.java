package com.orderflow.inventory.infrastructure.graphql;

import com.orderflow.inventory.domain.model.Product;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Controller
public class ProductGraphQlController {

    @SchemaMapping(typeName = "Product", field = "createdAt")
    public OffsetDateTime createdAt(Product product) {
        return product.getCreatedAt().atOffset(ZoneOffset.UTC);
    }

    @SchemaMapping(typeName = "Product", field = "updatedAt")
    public OffsetDateTime updatedAt(Product product) {
        return product.getUpdatedAt().atOffset(ZoneOffset.UTC);
    }
}
