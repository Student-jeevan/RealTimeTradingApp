package com.jeevan.TradingApp.config;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.boot.test.context.TestConfiguration;
import redis.embedded.RedisServer;

import java.io.IOException;

/**
 * Starts an embedded Redis server on port 6370 before ANY test bean is created.
 * Applied via @Import(RedisTestConfig.class) in unit tests that need Redis.
 *
 * Integration tests that use Testcontainers Redis should NOT import this class —
 * they override the port via @DynamicPropertySource.
 */
@TestConfiguration
public class RedisTestConfig {

    private RedisServer redisServer;

    @PostConstruct
    public void startRedis() throws IOException {
        redisServer = new RedisServer(6370);
        redisServer.start();
    }

    @PreDestroy
    public void stopRedis() {
        if (redisServer != null && redisServer.isActive()) {
            redisServer.stop();
        }
    }
}
