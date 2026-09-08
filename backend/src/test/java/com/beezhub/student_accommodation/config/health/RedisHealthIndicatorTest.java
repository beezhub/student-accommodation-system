package com.beezhub.student_accommodation.config.health;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisHealthIndicatorTest {

    @Mock
    private RedisConnectionFactory redisConnectionFactory;

    @Mock
    private RedisConnection redisConnection;

    private RedisHealthIndicator redisHealthIndicator;

    @BeforeEach
    void setUp() {
        redisHealthIndicator = new RedisHealthIndicator(redisConnectionFactory);
    }

    @Test
    void health_whenRedisUp_returnsUp() {
        when(redisConnectionFactory.getConnection()).thenReturn(redisConnection);
        when(redisConnection.ping()).thenReturn("PONG");

        Health health = redisHealthIndicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("ping", "PONG");
    }

    @Test
    void health_whenRedisDown_returnsDown() {
        when(redisConnectionFactory.getConnection())
                .thenThrow(new RuntimeException("Connection refused"));

        Health health = redisHealthIndicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsEntry("error", "Connection refused");
    }
}
