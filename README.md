# springboot-percentileconfig
A reproducer showing how Spring Boot config overwrites other config


To see the steps, look at the individual commits on main. 
Each commit should be runnable, and should export data. See the steps below:

1. In [b2885bf8](https://github.com/pirgeo/springboot-percentileconfig/commit/b2885bf8b6c77370709783bdd04894df3c45e802), we create a basic Timer instrument, that has one percentile, `[0.1]`.
   1. Output contains only 0.1 percentile:
   ```
   my.timer(TIMER)[]; count=0.0, total_time=0.0 seconds, max=0.0 seconds
   my.timer.percentile(GAUGE)[phi='0.1']; value=0.0 seconds
   ```
2. In [21caaa20](https://github.com/pirgeo/springboot-percentileconfig/commit/21caaa2059e3af26ca78cae54a3318d308f36514), we add a `MeterFilter` via Spring Beans, which extends the percentiles to `[0.1, 0.2]`.
   1. Output contains 0.1 and 0.2 percentile
   ```
   my.timer(TIMER)[]; count=0.0, total_time=0.0 seconds, max=0.0 seconds
   my.timer.percentile(GAUGE)[phi='0.2']; value=0.0 seconds
   my.timer.percentile(GAUGE)[phi='0.1']; value=0.0 seconds
   ```
3. In [d6568e3a](https://github.com/pirgeo/springboot-percentileconfig/commit/d6568e3a6eeeaf841c3268eb6ac6158529bd25b2), we add configuration to the Spring Boot configuration ([application.yaml](src/main/resources/application.yaml)). 
   This effectively overwrites the first two configurations.
   1. Output contains only 0.3 percentile
   ```
   my.timer(TIMER)[]; count=0.0, total_time=0.0 seconds, max=0.0 seconds
   my.timer.percentile(GAUGE)[phi='0.3']; value=0.0 seconds
   ```

As soon as the percentile configuration in the application.yaml is used, all other percentile configurations are ignored.
I think this is due to how MeterFilters are handled here: [MeterRegistry.java](https://github.com/micrometer-metrics/micrometer/blob/c69180d6819b1fdd278807e87eccdc9c0e16d333/micrometer-core/src/main/java/io/micrometer/core/instrument/MeterRegistry.java#L611-L655).
In the loop in [getOrCreateMeter](https://github.com/micrometer-metrics/micrometer/blob/c69180d6819b1fdd278807e87eccdc9c0e16d333/micrometer-core/src/main/java/io/micrometer/core/instrument/MeterRegistry.java#L629-L636), the `config` variable is overwritten with every new `DistributionStatisticConfig`, leading to only one of the configurations being applied (in this case the one supplied through the Spring Boot configuration). 
