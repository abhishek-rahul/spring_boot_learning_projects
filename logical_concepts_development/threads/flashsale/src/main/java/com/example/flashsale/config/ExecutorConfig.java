package com.example.flashsale.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.*;

@Configuration
public class ExecutorConfig {

    @Bean(name = "orderExecutor")
    public ExecutorService orderExecutor() {
        // Production-safe defaults for demo:
        // - bounded queue => prevents OOM
        // - CallerRunsPolicy => backpressure under load
        return new ThreadPoolExecutor(
                20,                  // core
                50,                  // max
                60, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(500),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    @Bean(name = "ioExecutor")
    public ExecutorService ioExecutor() {
        // Separate executor for simulated IO tasks (payment/email).
        return new ThreadPoolExecutor(
                10,
                30,
                60, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(300),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }
}
