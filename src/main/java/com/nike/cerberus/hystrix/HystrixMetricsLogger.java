package com.nike.cerberus.hystrix;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.netflix.hystrix.HystrixCircuitBreaker;
import com.netflix.hystrix.HystrixCommandMetrics;
import com.netflix.hystrix.HystrixThreadPoolMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Periodically print Hystrix metrics to the log.
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
            printHystrixCommandMetrics();
            printHystrixThreadPoolMetrics();
        } catch (Exception e) {
            LOGGER.warn("Error printing Hystrix metrics", e);
        }
    }

    public void printHystrixCommandMetrics() {
        for (HystrixCommandMetrics metrics : HystrixCommandMetrics.getInstances()) {
            boolean isCircuitOpen = HystrixCircuitBreaker.Factory.getInstance(metrics.getCommandKey()).isOpen();

            LOGGER.info("group:{}, commandKey:{}, CircuitOpen:{}, Mean:{}, 95%:{}, 99%:{}, 99.5%:{}, {}",
                    metrics.getCommandGroup().name(),
                    metrics.getCommandKey().name(),
                    isCircuitOpen,
                    metrics.getExecutionTimeMean(),
                    metrics.getExecutionTimePercentile(95.0),
                    metrics.getExecutionTimePercentile(99.5),
                    metrics.getExecutionTimePercentile(99.5),
                    metrics.getHealthCounts()
            );
        }
    }

    public void printHystrixThreadPoolMetrics() {
        for (HystrixThreadPoolMetrics metrics : HystrixThreadPoolMetrics.getInstances()) {
            LOGGER.info("threadPool:{}, rollingCounts[rejected:{}, executed:{}, maxActiveThreads:{}], cumulativeCounts[rejected:{}, executed:{}], {}",
                    metrics.getThreadPoolKey().name(),
                    metrics.getRollingCountThreadsRejected(),
                    metrics.getRollingCountThreadsExecuted(),
                    metrics.getRollingMaxActiveThreads(),
                    metrics.getCumulativeCountThreadsRejected(),
                    metrics.getCumulativeCountThreadsExecuted(),
                    metrics.getThreadPool()
            );
        }
    }

}