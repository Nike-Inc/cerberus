/*
 * Copyright (c) 2017 Nike, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nike.cerberus.jobs;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netflix.hystrix.HystrixCircuitBreaker;
import com.netflix.hystrix.HystrixCommandMetrics;
import com.netflix.hystrix.HystrixThreadPoolMetrics;
import com.nike.riposte.metrics.codahale.CodahaleMetricsCollector;
import com.nike.riposte.metrics.codahale.SignalFxAwareCodahaleMetricsCollector;
import com.signalfx.codahale.metrics.SettableLongGauge;
import org.knowm.sundial.Job;
import org.knowm.sundial.exceptions.JobInterruptException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Periodically print Hystrix metrics to the log.
 */
@Singleton
public class HystrixMetricsProcessingJob extends Job {

    private static final Logger log = LoggerFactory.getLogger(HystrixMetricsProcessingJob.class);

    private final CodahaleMetricsCollector metricsCollector;

    @Inject
    public HystrixMetricsProcessingJob(CodahaleMetricsCollector metricsCollector) {
        this.metricsCollector = metricsCollector;
    }

    @Override
    public void doRun() throws JobInterruptException {
        log.debug("Running hystrix metrics processing job");
        try {
            processHystrixCommandMetrics();
            processHystrixThreadPoolMetrics();
        } catch (JobInterruptException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Error processing Hystrix metrics", e);
        }
    }

    public void processHystrixCommandMetrics() {
        for (HystrixCommandMetrics metrics : HystrixCommandMetrics.getInstances()) {
            boolean isCircuitOpen = HystrixCircuitBreaker.Factory.getInstance(metrics.getCommandKey()).isOpen();

            log.debug("group:{}, commandKey:{}, CircuitOpen:{}, Mean:{}, 95%:{}, 99%:{}, 99.5%:{}, {}",
                    metrics.getCommandGroup().name(),
                    metrics.getCommandKey().name(),
                    isCircuitOpen,
                    metrics.getExecutionTimeMean(),
                    metrics.getExecutionTimePercentile(95.0),
                    metrics.getExecutionTimePercentile(99.0),
                    metrics.getExecutionTimePercentile(99.5),
                    metrics.getHealthCounts()
            );

            String baseMetricName = "cerberus.hystrix.command" +
                    metrics.getCommandGroup().name().toLowerCase() + "." +
                    metrics.getCommandKey().name().toLowerCase();

            getOrCreateSettableGauge(baseMetricName + ".circuitOpen").ifPresent((gauge) ->
                    gauge.setValue(isCircuitOpen ? 1 : 0));

            getOrCreateSettableGauge(baseMetricName + ".mean").ifPresent((gauge) ->
                    gauge.setValue(metrics.getExecutionTimeMean()));

            getOrCreateSettableGauge(baseMetricName + ".95th").ifPresent((gauge) ->
                    gauge.setValue(metrics.getExecutionTimePercentile(95.0)));

            getOrCreateSettableGauge(baseMetricName + ".99th").ifPresent((gauge) ->
                    gauge.setValue(metrics.getExecutionTimePercentile(99.0)));

            getOrCreateSettableGauge(baseMetricName + ".995th").ifPresent((gauge) ->
                    gauge.setValue(metrics.getExecutionTimePercentile(99.5)));

            getOrCreateSettableGauge(baseMetricName + ".totalCount").ifPresent((gauge) ->
                    gauge.setValue(metrics.getHealthCounts().getTotalRequests()));

            getOrCreateSettableGauge(baseMetricName + ".errorCount").ifPresent((gauge) ->
                    gauge.setValue(metrics.getHealthCounts().getErrorCount()));
        }
    }

    public void processHystrixThreadPoolMetrics() {
        for (HystrixThreadPoolMetrics metrics : HystrixThreadPoolMetrics.getInstances()) {
            log.debug("threadPool:{}, rollingCounts[rejected:{}, executed:{}, maxActiveThreads:{}], cumulativeCounts[rejected:{}, executed:{}], {}",
                    metrics.getThreadPoolKey().name(),
                    metrics.getRollingCountThreadsRejected(),
                    metrics.getRollingCountThreadsExecuted(),
                    metrics.getRollingMaxActiveThreads(),
                    metrics.getCumulativeCountThreadsRejected(),
                    metrics.getCumulativeCountThreadsExecuted(),
                    metrics.getThreadPool()
            );

            String baseMetricName = "cerberus.hystrix.threadPool." + metrics.getThreadPoolKey().name();

            getOrCreateSettableGauge(baseMetricName + ".rolling.rejected").ifPresent((gauge) ->
                    gauge.setValue(metrics.getRollingCountThreadsRejected()));

            getOrCreateSettableGauge(baseMetricName + ".rolling.executed").ifPresent((gauge) ->
                    gauge.setValue(metrics.getRollingCountThreadsExecuted()));

            getOrCreateSettableGauge(baseMetricName + ".rolling.maxActiveThreads").ifPresent((gauge) ->
                    gauge.setValue(metrics.getRollingMaxActiveThreads()));

            getOrCreateSettableGauge(baseMetricName + ".cumulative.rejected").ifPresent((gauge) ->
                    gauge.setValue(metrics.getCumulativeCountThreadsRejected()));

            getOrCreateSettableGauge(baseMetricName + ".cumulative.executed").ifPresent((gauge) ->
                    gauge.setValue(metrics.getCumulativeCountThreadsExecuted()));

            getOrCreateSettableGauge(baseMetricName + ".tpe.poolSize").ifPresent((gauge) ->
                    gauge.setValue(metrics.getThreadPool().getPoolSize()));

            getOrCreateSettableGauge(baseMetricName + ".tpe.activeThreads").ifPresent((gauge) ->
                    gauge.setValue(metrics.getThreadPool().getActiveCount()));

            getOrCreateSettableGauge(baseMetricName + ".tpe.queuedTasks").ifPresent((gauge) ->
                    gauge.setValue(metrics.getThreadPool().getQueue().size()));

            getOrCreateSettableGauge(baseMetricName + ".tpe.completedTasks").ifPresent((gauge) ->
                    gauge.setValue(metrics.getThreadPool().getCompletedTaskCount()));
        }
    }

    private Optional<SettableLongGauge> getOrCreateSettableGauge(String name) {
        boolean isGaugeAlreadyRegistered = metricsCollector.getMetricRegistry().getGauges().containsKey(name);
        if (isGaugeAlreadyRegistered) {
            return Optional.of((SettableLongGauge) metricsCollector.getMetricRegistry().getGauges().get(name));
        }

        try {
            return Optional.of(metricsCollector.getMetricRegistry().register(name, new SettableLongGauge()));
        } catch (IllegalArgumentException e) {
            log.error("Failed to get or create settable gauge, a non-gauge metric with name: {} is probably registered", name);
            return Optional.empty();
        }
    }

}