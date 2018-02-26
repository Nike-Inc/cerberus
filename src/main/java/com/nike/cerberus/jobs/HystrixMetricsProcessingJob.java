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

import com.google.common.collect.ImmutableMap;
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

import java.util.Map;

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
            Map<String, String> circuitDimensions = ImmutableMap.of(
                    "key", metrics.getCommandKey().name(),
                    "group", metrics.getCommandGroup().name()
            );
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


            metricsService.setLongGaugeValue("hystrix.command.circuit_open", isCircuitOpen ? 1 : 0, circuitDimensions);
            metricsService.setDoubleGaugeValue("hystrix.command.exec_time.mean", metrics.getExecutionTimeMean(), circuitDimensions);
            metricsService.setDoubleGaugeValue("hystrix.command.exec_time.95th", metrics.getExecutionTimePercentile(95.0), circuitDimensions);
            metricsService.setDoubleGaugeValue("hystrix.command.exec_time.99th", metrics.getExecutionTimePercentile(99.0), circuitDimensions);
            metricsService.setLongGaugeValue("hystrix.command.rolling.max_concurrent_execs", metrics.getRollingMaxConcurrentExecutions(), circuitDimensions);
            metricsService.setLongGaugeValue("hystrix.command.total_count", metrics.getHealthCounts().getTotalRequests(), circuitDimensions);
            metricsService.setLongGaugeValue("hystrix.command.error_count", metrics.getHealthCounts().getErrorCount(), circuitDimensions);
        }
    }

    public void processHystrixThreadPoolMetrics() {
        for (HystrixThreadPoolMetrics metrics : HystrixThreadPoolMetrics.getInstances()) {
            Map<String, String> dimensions = ImmutableMap.of(
                    "name", metrics.getThreadPoolKey().name()
            );
            log.debug("threadPool:{}, rollingCounts[rejected:{}, executed:{}, maxActiveThreads:{}], cumulativeCounts[rejected:{}, executed:{}], {}",
                    metrics.getThreadPoolKey().name(),
                    metrics.getRollingCountThreadsRejected(),
                    metrics.getRollingCountThreadsExecuted(),
                    metrics.getRollingMaxActiveThreads(),
                    metrics.getCumulativeCountThreadsRejected(),
                    metrics.getCumulativeCountThreadsExecuted(),
                    metrics.getThreadPool()
            );

            metricsService.setLongGaugeValue("hystrix.threads.rolling.rejected", metrics.getRollingCountThreadsRejected(), dimensions);
            metricsService.setLongGaugeValue("hystrix.threads.rolling.executed", metrics.getRollingCountThreadsExecuted(), dimensions);
            metricsService.setLongGaugeValue("hystrix.threads.rolling.maxActiveThreads", metrics.getRollingMaxActiveThreads(), dimensions);
            metricsService.setLongGaugeValue("hystrix.threads.cumulative.rejected", metrics.getCumulativeCountThreadsRejected(), dimensions);
            metricsService.setLongGaugeValue("hystrix.threads.cumulative.executed", metrics.getCumulativeCountThreadsExecuted(), dimensions);
            metricsService.setLongGaugeValue("hystrix.threads.activeThreads", metrics.getThreadPool().getActiveCount(), dimensions);
            metricsService.setLongGaugeValue("hystrix.threads.queuedTasks", metrics.getThreadPool().getQueue().size(), dimensions);
            metricsService.setLongGaugeValue("hystrix.threads.completedTasks", metrics.getThreadPool().getCompletedTaskCount(), dimensions);
        }
    }
}
