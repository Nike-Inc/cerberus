/*
 * Copyright (c) 2019 Nike, Inc.
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
import com.netflix.hystrix.HystrixCircuitBreaker;
import com.netflix.hystrix.HystrixCommandMetrics;
import com.netflix.hystrix.HystrixThreadPoolMetrics;
import com.nike.cerberus.metric.MetricsService;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Periodically print Hystrix metrics to the log. */
@Slf4j
@ConditionalOnProperty("cerberus.jobs.hystrixMetricsProcessingJob.enabled")
@Component
public class HystrixMetricsProcessingJob {
  private final MetricsService metricsService;

  @Autowired
  public HystrixMetricsProcessingJob(MetricsService metricsService) {
    this.metricsService = metricsService;
  }

  @Scheduled(cron = "${cerberus.jobs.hystrixMetricsProcessingJob.cronExpression}")
  public void execute() {
    log.debug("Running hystrix metrics processing job");
    try {
      processHystrixCommandMetrics();
      processHystrixThreadPoolMetrics();
    } catch (Exception e) {
      log.warn("Error processing Hystrix metrics", e);
    }
  }

  public void processHystrixCommandMetrics() {
    for (HystrixCommandMetrics metrics : HystrixCommandMetrics.getInstances()) {
      Map<String, String> dimensions =
          ImmutableMap.of(
              "key", metrics.getCommandKey().name(),
              "group", metrics.getCommandGroup().name());
      boolean isCircuitOpen =
          HystrixCircuitBreaker.Factory.getInstance(metrics.getCommandKey()).isOpen();

      log.debug(
          "group:{}, commandKey:{}, CircuitOpen:{}, Mean:{}, 95%:{}, 99%:{}, 99.5%:{}, {}",
          metrics.getCommandGroup().name(),
          metrics.getCommandKey().name(),
          isCircuitOpen,
          metrics.getExecutionTimeMean(),
          metrics.getExecutionTimePercentile(95.0),
          metrics.getExecutionTimePercentile(99.0),
          metrics.getExecutionTimePercentile(99.5),
          metrics.getHealthCounts());

      metricsService.getOrCreateCallbackGauge(
          "hystrix.command.circuit_open", () -> isCircuitOpen ? 1 : 0, dimensions);
      metricsService.getOrCreateCallbackGauge(
          "hystrix.command.exec_time.mean", metrics::getExecutionTimeMean, dimensions);
      metricsService.getOrCreateCallbackGauge(
          "hystrix.command.exec_time.95th",
          () -> metrics.getExecutionTimePercentile(95.0),
          dimensions);
      metricsService.getOrCreateCallbackGauge(
          "hystrix.command.exec_time.99th",
          () -> metrics.getExecutionTimePercentile(99.0),
          dimensions);
      metricsService.getOrCreateCallbackGauge(
          "hystrix.command.rolling.max_concurrent_execs",
          metrics::getRollingMaxConcurrentExecutions,
          dimensions);
      metricsService.getOrCreateCallbackGauge(
          "hystrix.command.total_count",
          () -> metrics.getHealthCounts().getTotalRequests(),
          dimensions);
      metricsService.getOrCreateCallbackGauge(
          "hystrix.command.error_count",
          () -> metrics.getHealthCounts().getErrorCount(),
          dimensions);
    }
  }

  public void processHystrixThreadPoolMetrics() {
    for (HystrixThreadPoolMetrics metrics : HystrixThreadPoolMetrics.getInstances()) {
      Map<String, String> dimensions = ImmutableMap.of("name", metrics.getThreadPoolKey().name());
      log.debug(
          "threadPool:{}, rollingCounts[rejected:{}, executed:{}, maxActiveThreads:{}], cumulativeCounts[rejected:{}, executed:{}], {}",
          metrics.getThreadPoolKey().name(),
          metrics.getRollingCountThreadsRejected(),
          metrics.getRollingCountThreadsExecuted(),
          metrics.getRollingMaxActiveThreads(),
          metrics.getCumulativeCountThreadsRejected(),
          metrics.getCumulativeCountThreadsExecuted(),
          metrics.getThreadPool());

      metricsService.getOrCreateCallbackGauge(
          "hystrix.threads.rolling.rejected", metrics::getRollingCountThreadsRejected, dimensions);
      metricsService.getOrCreateCallbackGauge(
          "hystrix.threads.rolling.executed", metrics::getRollingCountThreadsExecuted, dimensions);
      metricsService.getOrCreateCallbackGauge(
          "hystrix.threads.rolling.maxActiveThreads",
          metrics::getRollingMaxActiveThreads,
          dimensions);
      metricsService.getOrCreateCallbackGauge(
          "hystrix.threads.cumulative.rejected",
          metrics::getCumulativeCountThreadsRejected,
          dimensions);
      metricsService.getOrCreateCallbackGauge(
          "hystrix.threads.cumulative.executed",
          metrics::getCumulativeCountThreadsExecuted,
          dimensions);
      metricsService.getOrCreateCallbackGauge(
          "hystrix.threads.activeThreads",
          () -> metrics.getThreadPool().getActiveCount(),
          dimensions);
      metricsService.getOrCreateCallbackGauge(
          "hystrix.threads.queuedTasks",
          () -> metrics.getThreadPool().getQueue().size(),
          dimensions);
      metricsService.getOrCreateCallbackGauge(
          "hystrix.threads.completedTasks",
          () -> metrics.getThreadPool().getCompletedTaskCount(),
          dimensions);
    }
  }
}
