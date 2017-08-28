package com.nike.cerberus.hystrix;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.netflix.hystrix.HystrixCircuitBreaker;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommandMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Periodically print Hystrix metrics to the log
 */
@Singleton
public class HystrixMetricsLogger implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(HystrixMetricsLogger.class);

    private final ScheduledExecutorService executor;

    @Inject
    public HystrixMetricsLogger(@Named("hystrixExecutor") ScheduledExecutorService executor,
                                @Named("cms.hystrix.metricsLoggingInitialIntervalSeconds") long initialIntervalSeconds,
                                @Named("cms.hystrix.metricsLoggingIntervalSeconds") long intervalSeconds) {
        this.executor = executor;
        LOGGER.info("Hystrix metrics logging initialIntervalSeconds:{}, intervalSeconds:{}", initialIntervalSeconds, intervalSeconds);
        this.executor.scheduleWithFixedDelay(this, initialIntervalSeconds, intervalSeconds, TimeUnit.SECONDS);
    }

    @Override
    public void run() {
        try {
            printHystrixMetrics();
        } catch (Exception e) {
            LOGGER.warn("Error printing Hystrix metrics", e);
        }
    }

    public void printHystrixMetrics() {
        for (HystrixCommandMetrics metrics : HystrixCommandMetrics.getInstances()) {
            String groupName = metrics.getCommandGroup().name();
            HystrixCommandKey circuitKey = metrics.getCommandKey();
            boolean isCircuitOpen = HystrixCircuitBreaker.Factory.getInstance(metrics.getCommandKey()).isOpen();
            int mean = metrics.getExecutionTimeMean();
            int ninetyFifth = metrics.getExecutionTimePercentile(95.0);
            int ninetyNine = metrics.getExecutionTimePercentile(99.0);
            int ninetyNineFive = metrics.getExecutionTimePercentile(99.5);
            HystrixCommandMetrics.HealthCounts health = metrics.getHealthCounts();

            LOGGER.info("Hystrix metrics for group:{}, circuit:{}, CircuitOpen:{}, Mean:{}, 95%:{}, 99%:{}, 99.5%:{}, {}",
                    groupName, circuitKey.name(), isCircuitOpen, mean, ninetyFifth, ninetyNine, ninetyNineFive, health);
        }
    }
}