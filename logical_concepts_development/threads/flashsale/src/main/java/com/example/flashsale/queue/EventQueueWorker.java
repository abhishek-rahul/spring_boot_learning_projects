package com.example.flashsale.queue;

import com.example.flashsale.metrics.Metrics;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import java.util.concurrent.*;

@Component
public class EventQueueWorker {

    private final BlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(1000);
    private final ExecutorService workerPool = Executors.newFixedThreadPool(2);
    private final Metrics metrics;

    private volatile boolean running = true;

    public EventQueueWorker(Metrics metrics) {
        this.metrics = metrics;
        startWorkers();
    }

    private void startWorkers() {
        for (int i = 0; i < 2; i++) {
            workerPool.submit(() -> {
                while (running && !Thread.currentThread().isInterrupted()) {
                    try {
                        Runnable job = queue.take(); // blocks if empty
                        job.run();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    } catch (Exception ex) {
                        // In production: log properly
                    }
                }
            });
        }
    }

    public boolean submit(Runnable job) {
        boolean ok = queue.offer(job);
        if (ok) {
            metrics.emailsQueued.incrementAndGet();
        } else {
            metrics.emailQueueDropped.incrementAndGet();
        }
        return ok;
    }

    @PreDestroy
    public void shutdown() {
        running = false; // volatile visibility
        workerPool.shutdownNow();
    }
}
