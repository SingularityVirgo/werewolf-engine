package com.werewolfengine.gateway;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class RoomExecutionGuardTest {

    @Test
    void execute_serializesPerRoom() throws InterruptedException {
        RoomExecutionGuard guard = new RoomExecutionGuard();
        AtomicInteger counter = new AtomicInteger();
        int threads = 8;
        int incrementsPerThread = 500;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    start.await();
                    for (int j = 0; j < incrementsPerThread; j++) {
                        guard.execute("r_test", () -> {
                            int v = counter.get();
                            counter.set(v + 1);
                            return null;
                        });
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown();
        done.await();
        pool.shutdown();

        assertThat(counter.get()).isEqualTo(threads * incrementsPerThread);
    }

    @Test
    void execute_allowsParallelAcrossRooms() throws InterruptedException {
        RoomExecutionGuard guard = new RoomExecutionGuard();
        AtomicInteger a = new AtomicInteger();
        AtomicInteger b = new AtomicInteger();
        CountDownLatch bothIncremented = new CountDownLatch(2);

        Thread t1 = new Thread(() -> guard.execute("r_a", () -> {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            a.incrementAndGet();
            bothIncremented.countDown();
            return null;
        }));
        Thread t2 = new Thread(() -> guard.execute("r_b", () -> {
            b.incrementAndGet();
            bothIncremented.countDown();
            return null;
        }));

        t1.start();
        t2.start();
        bothIncremented.await();
        t1.join();
        t2.join();

        assertThat(a.get()).isEqualTo(1);
        assertThat(b.get()).isEqualTo(1);
    }
}
