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
import com.nike.cerberus.service.MetricsService;
import org.knowm.sundial.Job;
import org.knowm.sundial.exceptions.JobInterruptException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Periodically print Hystrix metrics to the log.
 */
@Singleton
public class HystrixMetricsProcessingJob extends Job {

    private static final Logger log = LoggerFactory.getLogger(HystrixMetricsProcessingJob.class);

    private final MetricsService metricsService;

    @Inject
    public HystrixMetricsProcessingJob(MetricsService metricsService) {
        this.metricsService = metricsService;
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

            String baseMetricName = "hyst.cmd." +
                    metrics.getCommandGroup().name().toLowerCase() + "." +
                    metrics.getCommandKey().name().toLowerCase();

            metricsService.setGaugeValue(baseMetricName + ".circuitOpen", isCircuitOpen ? 1 : 0);
            metricsService.setGaugeValue(baseMetricName + ".mean", metrics.getExecutionTimeMean());
            metricsService.setGaugeValue(baseMetricName + ".95th", metrics.getExecutionTimePercentile(95.0));
            metricsService.setGaugeValue(baseMetricName + ".99th", metrics.getExecutionTimePercentile(99.0));
            metricsService.setGaugeValue(baseMetricName + ".995th", metrics.getExecutionTimePercentile(99.5));
            metricsService.setGaugeValue(baseMetricName + ".totalCount", metrics.getHealthCounts().getTotalRequests());
            metricsService.setGaugeValue(baseMetricName + ".errorCount", metrics.getHealthCounts().getErrorCount());
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

            String baseMetricName = "hyst.tP." + metrics.getThreadPoolKey().name();

            metricsService.setGaugeValue(baseMetricName + ".rol.rejected", metrics.getRollingCountThreadsRejected());
            metricsService.setGaugeValue(baseMetricName + ".rol.executed", metrics.getRollingCountThreadsExecuted());
            metricsService.setGaugeValue(baseMetricName + ".rol.maxActiveThreads", metrics.getRollingMaxActiveThreads());
            metricsService.setGaugeValue(baseMetricName + ".cum.rejected", metrics.getCumulativeCountThreadsRejected());
            metricsService.setGaugeValue(baseMetricName + ".cum.executed", metrics.getCumulativeCountThreadsExecuted());
            metricsService.setGaugeValue(baseMetricName + ".tpe.poolSize", metrics.getThreadPool().getPoolSize());
            metricsService.setGaugeValue(baseMetricName + ".tpe.activeThreads", metrics.getThreadPool().getActiveCount());
            metricsService.setGaugeValue(baseMetricName + ".tpe.queuedTasks", metrics.getThreadPool().getQueue().size());
            metricsService.setGaugeValue(baseMetricName + ".tpe.completedTasks", metrics.getThreadPool().getCompletedTaskCount());
        }
    }
}
