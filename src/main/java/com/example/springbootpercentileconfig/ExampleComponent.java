package com.example.springbootpercentileconfig;


import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class ExampleComponent {

    private final ExecutorService executorService;

    private static final Logger logger = LoggerFactory.getLogger(ExampleComponent.class);

    public ExampleComponent(MeterRegistry registry) {

        executorService = Executors.newFixedThreadPool(2);

        // start the timer with one percentile, 0.1 using the builder.
        Timer.builder("my.timer")
                .publishPercentiles(0.1)
                .register(registry);

        // record data on the timer in a background thread.
        executorService.submit(() -> {
            while (true) {
                // record random durations between 0 and 5s every 1s
                registry.timer("my.timer").record(Duration.ofMillis(ThreadLocalRandom.current().nextLong(5_000)));
                Thread.sleep(1_000);
            }
        });

        // Print meters every 10 seconds from a background thread.
        executorService.submit(() -> {
           while (true) {
               if (registry instanceof SimpleMeterRegistry){
                    logger.info(((SimpleMeterRegistry) registry).getMetersAsString());
               }
               else {
                    logger.warn("MeterRegistry is not a SimpleMeterRegistry, skipping printing of meters");
               }
               Thread.sleep(10_000);
           }
        });
    }
}
