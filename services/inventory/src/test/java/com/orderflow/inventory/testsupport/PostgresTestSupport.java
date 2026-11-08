package com.orderflow.inventory.testsupport;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

public abstract class PostgresTestSupport {
    private static final GenericContainer<?> POSTGRES = new GenericContainer<>(DockerImageName.parse("postgres:16-alpine"))
            .withEnv("POSTGRES_DB", "inventory_test")
            .withEnv("POSTGRES_USER", "inventory")
            .withEnv("POSTGRES_PASSWORD", "inventory")
            .withExposedPorts(5432)
            .waitingFor(Wait.forListeningPort());

    static { POSTGRES.start(); }

    @DynamicPropertySource
    protected static void configurePostgres(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:postgresql://" + POSTGRES.getHost() + ":" + POSTGRES.getMappedPort(5432) + "/inventory_test");
        registry.add("spring.datasource.username", () -> "inventory");
        registry.add("spring.datasource.password", () -> "inventory");
    }
}
