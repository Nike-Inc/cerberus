/*
 * Copyright (c) 2020 Nike, inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nike.cerberus.metric;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Slf4jReporter;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LoggingMetricsService implements MetricsService {

  private final MetricRegistry metricRegistry;

  public LoggingMetricsService(Slf4jReporter.LoggingLevel level, long period, TimeUnit timeUnit) {

    metricRegistry = new MetricRegistry();

    Slf4jReporter.forRegistry(metricRegistry)
        .outputTo(log)
        .withLoggingLevel(level)
        .scheduleOn(Executors.newSingleThreadScheduledExecutor())
        .build()
        .start(period, timeUnit);
  }

  @Override
  public Counter getOrCreateCounter(String name, Map<String, String> dimensions) {
    return metricRegistry.counter(getMetricNameFromNameAndDimensions(name, dimensions));
  }

  @Override
  public Gauge getOrCreateCallbackGauge(
      String name, Supplier<Number> supplier, Map<String, String> dimensions) {
    return metricRegistry.gauge(
        getMetricNameFromNameAndDimensions(name, dimensions), () -> supplier::get);
  }

  private String getMetricNameFromNameAndDimensions(
      String name, Map<String, String> optionalDimensions) {
    var metricNameBuilder = new StringBuilder(name);
    Optional.ofNullable(optionalDimensions)
        .ifPresent(
            dimensions -> {
              metricNameBuilder.append('(');
              dimensions.forEach(
                  (key, value) ->
                      metricNameBuilder
                          .append('[')
                          .append(key)
                          .append(":")
                          .append(value)
                          .append(']'));
              metricNameBuilder.append(')');
            });
    return metricNameBuilder.toString();
  }
}
