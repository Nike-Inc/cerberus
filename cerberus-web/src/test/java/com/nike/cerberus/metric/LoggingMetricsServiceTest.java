package com.nike.cerberus.metric;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Slf4jReporter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class LoggingMetricsServiceTest {

  private LoggingMetricsService loggingMetricsService;

  private MetricRegistry metricRegistry;

  @Before
  public void setup() {
    loggingMetricsService =
        new LoggingMetricsService(Slf4jReporter.LoggingLevel.INFO, 5, TimeUnit.MINUTES);
    metricRegistry = Mockito.mock(MetricRegistry.class);
    loggingMetricsService.setMetricRegistry(metricRegistry);
  }

  @Test
  public void testGetOrCreateCounter() {
    Counter counter = Mockito.mock(Counter.class);
    Mockito.when(metricRegistry.counter("name")).thenReturn(counter);
    Counter actualCounter = loggingMetricsService.getOrCreateCounter("name", null);
    Assert.assertSame(counter, actualCounter);
  }

  @Test
  public void testGetOrCreateCounterWithDimensions() {
    Counter counter = Mockito.mock(Counter.class);
    Map<String, String> dimensions = new HashMap<>();
    dimensions.put("key", "value");
    Mockito.when(metricRegistry.counter("name([key:value])")).thenReturn(counter);
    Counter actualCounter = loggingMetricsService.getOrCreateCounter("name", dimensions);
    Assert.assertSame(counter, actualCounter);
  }

  @Test
  public void testGetOrCreateCallbackGauge() {
    Gauge gauge = Mockito.mock(Gauge.class);
    Mockito.when(
            metricRegistry.gauge(
                Mockito.eq("name"), Mockito.any(MetricRegistry.MetricSupplier.class)))
        .thenReturn(gauge);
    Gauge actualGauge = loggingMetricsService.getOrCreateCallbackGauge("name", () -> 1, null);
    Assert.assertSame(gauge, actualGauge);
  }
}
