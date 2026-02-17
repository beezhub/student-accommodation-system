package com.beezhub.student_accommodation.service.tenant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link TenantContext}.
 * No Spring context needed — TenantContext is a pure static ThreadLocal utility.
 */
class TenantContextTest {

    @AfterEach
    void tearDown() {
        // Always clear TenantContext after each test to avoid state leakage between tests
        TenantContext.clear();
    }

    // ────────────────────────────────────────────────────────────
    // set / get
    // ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("setTenantId_getTenantId_returnsCorrectValue")
    void setTenantId_getTenantId_returnsCorrectValue() {
        TenantContext.setTenantId(42L);

        assertThat(TenantContext.getTenantId()).isEqualTo(42L);
    }

    @Test
    @DisplayName("getTenantId_neverSet_returnsNull")
    void getTenantId_neverSet_returnsNull() {
        assertThat(TenantContext.getTenantId()).isNull();
    }

    @Test
    @DisplayName("setTenantId_calledTwice_overwritesPreviousValue")
    void setTenantId_calledTwice_overwritesPreviousValue() {
        TenantContext.setTenantId(1L);
        TenantContext.setTenantId(99L);

        assertThat(TenantContext.getTenantId()).isEqualTo(99L);
    }

    // ────────────────────────────────────────────────────────────
    // clear
    // ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("clear_afterSet_returnsNull")
    void clear_afterSet_returnsNull() {
        TenantContext.setTenantId(1L);

        TenantContext.clear();

        assertThat(TenantContext.getTenantId()).isNull();
    }

    @Test
    @DisplayName("clear_calledWithoutPriorSet_doesNotThrow")
    void clear_calledWithoutPriorSet_doesNotThrow() {
        // Should not throw even when no value was set
        TenantContext.clear();

        assertThat(TenantContext.getTenantId()).isNull();
    }

    // ────────────────────────────────────────────────────────────
    // Thread isolation — the core correctness guarantee of ThreadLocal
    // ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("threadIsolation_separateThreads_independentValues")
    void threadIsolation_separateThreads_independentValues() throws InterruptedException {
        TenantContext.setTenantId(1L);

        AtomicReference<Long> valueInOtherThread = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        Thread other = new Thread(() -> {
            // This thread has never called setTenantId — should see null
            valueInOtherThread.set(TenantContext.getTenantId());
            latch.countDown();
        });
        other.start();
        latch.await();

        // Main thread still has its own value
        assertThat(TenantContext.getTenantId()).isEqualTo(1L);
        // Other thread saw null (ThreadLocal isolation)
        assertThat(valueInOtherThread.get()).isNull();
    }

    @Test
    @DisplayName("threadIsolation_eachThreadSetsOwnValue_noInterference")
    void threadIsolation_eachThreadSetsOwnValue_noInterference() throws InterruptedException {
        TenantContext.setTenantId(100L);

        AtomicReference<Long> valueInOtherThread = new AtomicReference<>();
        CountDownLatch ready = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(1);

        Thread other = new Thread(() -> {
            TenantContext.setTenantId(200L);
            valueInOtherThread.set(TenantContext.getTenantId());
            ready.countDown();
            try { done.await(); } catch (InterruptedException ignored) {}
            TenantContext.clear();
        });
        other.start();
        ready.await();

        // Each thread retains its own value independently
        assertThat(TenantContext.getTenantId()).isEqualTo(100L);
        assertThat(valueInOtherThread.get()).isEqualTo(200L);

        done.countDown();
    }
}
