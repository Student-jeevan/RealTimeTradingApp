package com.jeevan.TradingApp.integration;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Base class for ALL integration tests.
 *
 * Starts 3 containers (shared across the entire test suite via static fields):
 *  - MySQL 8  → replaced instead of H2 for full SQL compatibility
 *  - Redis 7  → real Lettuce client against actual Redis
 *  - Kafka    → real broker via Confluent KafkaContainer
 *
 * @DynamicPropertySource injects the container hostnames/ports into
 * Spring's application context before beans are created.
 *
 * @DirtiesContext = AFTER_CLASS resets the Spring context after each
 * test class so containers are still shared but state is fresh.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SuppressWarnings("resource") // Eclipse false-positive: @Container fields are managed by @Testcontainers lifecycle
public abstract class BaseIntegrationTest {

    // ── MySQL container  ──────────────────────────────────────────────────────
    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.33")
            .withDatabaseName("trading_test")
            .withUsername("test_user")
            .withPassword("test_pass")
            .withReuse(true);   // reuse across test classes for speed

    // ── Redis container ───────────────────────────────────────────────────────
    @Container
    static final GenericContainer<?> REDIS =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                    .withExposedPorts(6379)
                    .withReuse(true);

    // ── Kafka container ───────────────────────────────────────────────────────
    @Container
    static final KafkaContainer KAFKA =
            new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"))
                    .withReuse(true);

    // ── Dynamic property injection ────────────────────────────────────────────
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // MySQL
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        registry.add("spring.jpa.database-platform",
                () -> "org.hibernate.dialect.MySQL8Dialect");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");

        // Redis
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port",
                () -> REDIS.getMappedPort(6379));

        // Kafka
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
    }
}
