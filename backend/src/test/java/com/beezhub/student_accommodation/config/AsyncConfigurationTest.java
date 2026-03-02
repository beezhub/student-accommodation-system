package com.beezhub.student_accommodation.config;

import com.beezhub.student_accommodation.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.context.TestPropertySource;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for AsyncConfiguration to verify async executor pool sizes
 * and configuration loading.
 *
 * Refs: TASK-1.10, ARCH-2.3.4, ARCH-2.5-PB-003
 */
@TestPropertySource(properties = {
        "async.core-pool-size=10",
        "async.max-pool-size=20",
        "async.queue-capacity=500"
})
class AsyncConfigurationTest extends AbstractIntegrationTest {

    @Autowired
    private AsyncConfiguration asyncConfiguration;

    @Test
    @DisplayName("Notification executor is configured with the correct core pool size")
    void asyncExecutor_loadsWithCorrectCorePoolSize() {
        Executor executor = asyncConfiguration.notificationExecutor();

        assertThat(executor).isInstanceOf(ThreadPoolTaskExecutor.class);
        ThreadPoolTaskExecutor taskExecutor = (ThreadPoolTaskExecutor) executor;
        assertThat(taskExecutor.getCorePoolSize()).isEqualTo(10);
    }

    @Test
    @DisplayName("Notification executor is configured with the correct max pool size")
    void asyncExecutor_loadsWithCorrectMaxPoolSize() {
        Executor executor = asyncConfiguration.notificationExecutor();

        assertThat(executor).isInstanceOf(ThreadPoolTaskExecutor.class);
        ThreadPoolTaskExecutor taskExecutor = (ThreadPoolTaskExecutor) executor;
        assertThat(taskExecutor.getMaxPoolSize()).isEqualTo(20);
    }

    @Test
    @DisplayName("Notification executor is configured with the correct queue capacity")
    void asyncExecutor_loadsWithCorrectQueueCapacity() {
        Executor executor = asyncConfiguration.notificationExecutor();

        assertThat(executor).isInstanceOf(ThreadPoolTaskExecutor.class);
        ThreadPoolTaskExecutor taskExecutor = (ThreadPoolTaskExecutor) executor;
        ThreadPoolExecutor threadPoolExecutor = taskExecutor.getThreadPoolExecutor();
        assertThat(threadPoolExecutor.getQueue().remainingCapacity()).isEqualTo(500);
    }

    @Test
    @DisplayName("Notification executor uses CallerRunsPolicy as rejection handler")
    void asyncExecutor_hasCallerRunsPolicyAsRejectionHandler() {
        Executor executor = asyncConfiguration.notificationExecutor();

        assertThat(executor).isInstanceOf(ThreadPoolTaskExecutor.class);
        ThreadPoolTaskExecutor taskExecutor = (ThreadPoolTaskExecutor) executor;
        ThreadPoolExecutor threadPoolExecutor = taskExecutor.getThreadPoolExecutor();
        assertThat(threadPoolExecutor.getRejectedExecutionHandler())
                .isInstanceOf(ThreadPoolExecutor.CallerRunsPolicy.class);
    }

    @Test
    @DisplayName("Notification executor thread name prefix is 'notification-async-'")
    void asyncExecutor_hasCorrectThreadNamePrefix() {
        Executor executor = asyncConfiguration.notificationExecutor();

        assertThat(executor).isInstanceOf(ThreadPoolTaskExecutor.class);
        ThreadPoolTaskExecutor taskExecutor = (ThreadPoolTaskExecutor) executor;
        assertThat(taskExecutor.getThreadNamePrefix()).isEqualTo("notification-async-");
    }

    @Test
    @DisplayName("Notification executor waits for tasks to complete on shutdown")
    void asyncExecutor_waitsForTasksToCompleteOnShutdown() {
        Executor executor = asyncConfiguration.notificationExecutor();

        assertThat(executor).isInstanceOf(ThreadPoolTaskExecutor.class);
        ThreadPoolTaskExecutor taskExecutor = (ThreadPoolTaskExecutor) executor;
        assertThat(taskExecutor.getThreadPoolExecutor()).isNotNull();
    }
}
