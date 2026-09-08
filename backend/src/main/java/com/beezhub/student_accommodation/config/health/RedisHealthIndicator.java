package com.beezhub.student_accommodation.config.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Component;

@Component
public class RedisHealthIndicator implements HealthIndicator {

    private final RedisConnectionFactory redisConnectionFactory;

    public RedisHealthIndicator(RedisConnectionFactory redisConnectionFactory) {
        this.redisConnectionFactory = redisConnectionFactory;
    }

    @Override
    public Health health() {
        try {
            var connection = redisConnectionFactory.getConnection();
            String pong = connection.ping();
            connection.close();
            return Health.up()
                    .withDetail("ping", pong)
                    .build();
        } catch (Exception e) {
            return Health.down(e)
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
