package com.example.springbootpercentileconfig;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@SpringBootApplication
public class SpringBootPercentileConfigApplication {

    private static final Logger logger = LoggerFactory.getLogger(SpringBootPercentileConfigApplication.class);

    @Bean
    public MeterRegistryCustomizer<SimpleMeterRegistry> percentileCustomizer() {
        return registry -> registry.config().meterFilter(createPercentileMeterFilter());
    }

//    Also works to create the MeterFilter, will then apply to all registries as compared to the above which only applies to SimpleMeterRegistry
//    @Bean
//    public MeterFilter configurePercentile() {
//        return createPercentileMeterFilter();
//    }


    public static void main(String[] args) {
        SpringApplication.run(SpringBootPercentileConfigApplication.class, args);
    }


    private static MeterFilter createPercentileMeterFilter() {
        return new MeterFilter() {
            @Override
            public DistributionStatisticConfig configure(Meter.Id id, DistributionStatisticConfig config) {
                if (config.getPercentiles() == null) {
                    return config;
                }

                logger.info("passed in percentiles: " + Arrays.toString(config.getPercentiles()));
                if (id.getName().equals("my.timer")) {
                    // add a 0.2 percentile
                    double[] newPercentiles = addPercentile(config.getPercentiles(), 0.2);

                    // keep the rest of the other config, but set the updated percentiles.
                    DistributionStatisticConfig merged = DistributionStatisticConfig.builder()
                            .percentiles(newPercentiles)
                            .build()
                            .merge(config);
                    return merged;
                }
                return config;
            }


        };
    }

    private static double[] addPercentile(double[] oldPercentiles, double newPercentile) {
        List<Double> newPercentiles = Arrays.stream(oldPercentiles).boxed().collect(Collectors.toList());

        if (newPercentiles.contains(newPercentile)) {
            return oldPercentiles;
        }

        newPercentiles.add(newPercentile);
        Collections.sort(newPercentiles);

        return newPercentiles.stream().mapToDouble(x -> x).toArray();
    }
}
