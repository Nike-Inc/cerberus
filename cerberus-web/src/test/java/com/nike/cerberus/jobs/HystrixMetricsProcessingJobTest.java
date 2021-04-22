package com.nike.cerberus.jobs;

import com.google.common.collect.ImmutableMap;
import com.netflix.hystrix.*;
import com.nike.cerberus.metric.MetricsService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Supplier;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class HystrixMetricsProcessingJobTest {

  @Mock private MetricsService metricsService;

  @InjectMocks private HystrixMetricsProcessingJob hystrixMetricsProcessingJob;

  @Captor private ArgumentCaptor<Supplier<Number>> supplierArgumentCaptor;

  @Captor private ArgumentCaptor<Map<String, String>> mapArgumentCaptor;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void testExecute() {
    HystrixMetricsProcessingJob hystrixMetricsProcessingJobSpy =
        Mockito.spy(hystrixMetricsProcessingJob);
    List<HystrixCommandMetrics> hystrixCommandMetricsList = getHystrixCommandMetricsList();
    Mockito.doReturn(hystrixCommandMetricsList)
        .when(hystrixMetricsProcessingJobSpy)
        .getHystrixCommandMetrics();
    HystrixCircuitBreaker hystrixCircuitBreaker = Mockito.mock(HystrixCircuitBreaker.class);
    HystrixCommandKey commandKey = hystrixCommandMetricsList.get(0).getCommandKey();
    Mockito.doReturn(hystrixCircuitBreaker)
        .when(hystrixMetricsProcessingJobSpy)
        .getHystrixCircuitBreaker(commandKey);
    List<HystrixThreadPoolMetrics> hystrixThreadPoolMetrics = getHystrixThreadPoolMetrics();
    Mockito.doReturn(hystrixThreadPoolMetrics)
        .when(hystrixMetricsProcessingJobSpy)
        .getHystrixThreadPoolMetrics();
    hystrixMetricsProcessingJobSpy.execute();
    verifyCommandMetricsOperations();
    verifyThreadPoolOperations();
  }

  private List<HystrixCommandMetrics> getHystrixCommandMetricsList() {
    List<HystrixCommandMetrics> hystrixCommandMetricsList = new ArrayList<>();
    HystrixCommandMetrics hystrixCommandMetrics = Mockito.mock(HystrixCommandMetrics.class);
    HystrixCommandKey hystrixCommandKey = Mockito.mock(HystrixCommandKey.class);
    Mockito.when(hystrixCommandKey.name()).thenReturn("name");
    Mockito.when(hystrixCommandMetrics.getCommandKey()).thenReturn(hystrixCommandKey);
    HystrixCommandGroupKey hystrixCommandGroupKey = Mockito.mock(HystrixCommandGroupKey.class);
    Mockito.when(hystrixCommandGroupKey.name()).thenReturn("groupKey");
    Mockito.when(hystrixCommandMetrics.getCommandGroup()).thenReturn(hystrixCommandGroupKey);
    HystrixCommandMetrics.HealthCounts healthCounts = HystrixCommandMetrics.HealthCounts.empty();
    Mockito.when(hystrixCommandMetrics.getHealthCounts()).thenReturn(healthCounts);
    Mockito.when(hystrixCommandMetrics.getExecutionTimeMean()).thenReturn(100);
    Mockito.when(hystrixCommandMetrics.getExecutionTimePercentile(95.0)).thenReturn(95);
    Mockito.when(hystrixCommandMetrics.getExecutionTimePercentile(99.0)).thenReturn(99);
    Mockito.when(hystrixCommandMetrics.getExecutionTimePercentile(99.5)).thenReturn(100);
    Mockito.when(hystrixCommandMetrics.getRollingMaxConcurrentExecutions()).thenReturn(43l);
    hystrixCommandMetricsList.add(hystrixCommandMetrics);
    return hystrixCommandMetricsList;
  }

  private void verifyCommandMetricsOperations() {
    Map<String, String> dimensions =
        ImmutableMap.of(
            "key", "name",
            "group", "groupKey");
    Mockito.verify(metricsService)
        .getOrCreateCallbackGauge(
            Mockito.eq("hystrix.command.circuit_open"),
            supplierArgumentCaptor.capture(),
            mapArgumentCaptor.capture());
    Assert.assertEquals(0, supplierArgumentCaptor.getValue().get());
    Assert.assertEquals(dimensions, mapArgumentCaptor.getValue());
    Mockito.verify(metricsService)
        .getOrCreateCallbackGauge(
            Mockito.eq("hystrix.command.exec_time.mean"),
            supplierArgumentCaptor.capture(),
            mapArgumentCaptor.capture());
    Assert.assertEquals(100, supplierArgumentCaptor.getValue().get());
    Assert.assertEquals(dimensions, mapArgumentCaptor.getValue());
    Mockito.verify(metricsService)
        .getOrCreateCallbackGauge(
            Mockito.eq("hystrix.command.exec_time.95th"),
            supplierArgumentCaptor.capture(),
            mapArgumentCaptor.capture());
    Assert.assertEquals(95, supplierArgumentCaptor.getValue().get());
    Assert.assertEquals(dimensions, mapArgumentCaptor.getValue());
    Mockito.verify(metricsService)
        .getOrCreateCallbackGauge(
            Mockito.eq("hystrix.command.exec_time.99th"),
            supplierArgumentCaptor.capture(),
            mapArgumentCaptor.capture());
    Assert.assertEquals(99, supplierArgumentCaptor.getValue().get());
    Assert.assertEquals(dimensions, mapArgumentCaptor.getValue());
    Mockito.verify(metricsService)
        .getOrCreateCallbackGauge(
            Mockito.eq("hystrix.command.rolling.max_concurrent_execs"),
            supplierArgumentCaptor.capture(),
            mapArgumentCaptor.capture());
    Assert.assertEquals(43l, supplierArgumentCaptor.getValue().get());
    Assert.assertEquals(dimensions, mapArgumentCaptor.getValue());
    Mockito.verify(metricsService)
        .getOrCreateCallbackGauge(
            Mockito.eq("hystrix.command.total_count"),
            supplierArgumentCaptor.capture(),
            mapArgumentCaptor.capture());
    Assert.assertEquals(0l, supplierArgumentCaptor.getValue().get());
    Assert.assertEquals(dimensions, mapArgumentCaptor.getValue());
    Mockito.verify(metricsService)
        .getOrCreateCallbackGauge(
            Mockito.eq("hystrix.command.error_count"),
            supplierArgumentCaptor.capture(),
            mapArgumentCaptor.capture());
    Assert.assertEquals(0l, supplierArgumentCaptor.getValue().get());
    Assert.assertEquals(dimensions, mapArgumentCaptor.getValue());
  }

  private List<HystrixThreadPoolMetrics> getHystrixThreadPoolMetrics() {
    List<HystrixThreadPoolMetrics> hystrixCommandMetricsList = new ArrayList<>();
    HystrixThreadPoolMetrics hystrixThreadPoolMetrics =
        Mockito.mock(HystrixThreadPoolMetrics.class);
    hystrixCommandMetricsList.add(hystrixThreadPoolMetrics);
    HystrixThreadPoolKey hystrixThreadPoolKey = Mockito.mock(HystrixThreadPoolKey.class);
    Mockito.when(hystrixThreadPoolKey.name()).thenReturn("name");
    Mockito.when(hystrixThreadPoolMetrics.getThreadPoolKey()).thenReturn(hystrixThreadPoolKey);
    Mockito.when(hystrixThreadPoolMetrics.getRollingCountThreadsRejected()).thenReturn(100l);
    Mockito.when(hystrixThreadPoolMetrics.getRollingCountThreadsExecuted()).thenReturn(200l);
    Mockito.when(hystrixThreadPoolMetrics.getRollingMaxActiveThreads()).thenReturn(300l);
    Mockito.when(hystrixThreadPoolMetrics.getCumulativeCountThreadsRejected()).thenReturn(400l);
    Mockito.when(hystrixThreadPoolMetrics.getCumulativeCountThreadsExecuted()).thenReturn(500l);
    ThreadPoolExecutor threadPoolExecutor = Mockito.mock(ThreadPoolExecutor.class);
    Mockito.when(threadPoolExecutor.getActiveCount()).thenReturn(30);
    Mockito.when(threadPoolExecutor.getCompletedTaskCount()).thenReturn(40l);
    BlockingQueue blockingQueue = Mockito.mock(BlockingQueue.class);
    Mockito.when(blockingQueue.size()).thenReturn(10);
    Mockito.when(threadPoolExecutor.getQueue()).thenReturn(blockingQueue);
    Mockito.when(hystrixThreadPoolMetrics.getThreadPool()).thenReturn(threadPoolExecutor);
    return hystrixCommandMetricsList;
  }

  private void verifyThreadPoolOperations() {
    Map<String, String> dimensions = ImmutableMap.of("name", "name");
    Mockito.verify(metricsService)
        .getOrCreateCallbackGauge(
            Mockito.eq("hystrix.threads.rolling.rejected"),
            supplierArgumentCaptor.capture(),
            mapArgumentCaptor.capture());
    Assert.assertEquals(100l, supplierArgumentCaptor.getValue().get());
    Assert.assertEquals(dimensions, mapArgumentCaptor.getValue());
    Mockito.verify(metricsService)
        .getOrCreateCallbackGauge(
            Mockito.eq("hystrix.threads.rolling.executed"),
            supplierArgumentCaptor.capture(),
            mapArgumentCaptor.capture());
    Assert.assertEquals(200l, supplierArgumentCaptor.getValue().get());
    Assert.assertEquals(dimensions, mapArgumentCaptor.getValue());
    Mockito.verify(metricsService)
        .getOrCreateCallbackGauge(
            Mockito.eq("hystrix.threads.rolling.maxActiveThreads"),
            supplierArgumentCaptor.capture(),
            mapArgumentCaptor.capture());
    Assert.assertEquals(300l, supplierArgumentCaptor.getValue().get());
    Assert.assertEquals(dimensions, mapArgumentCaptor.getValue());
    Mockito.verify(metricsService)
        .getOrCreateCallbackGauge(
            Mockito.eq("hystrix.threads.cumulative.rejected"),
            supplierArgumentCaptor.capture(),
            mapArgumentCaptor.capture());
    Assert.assertEquals(400l, supplierArgumentCaptor.getValue().get());
    Assert.assertEquals(dimensions, mapArgumentCaptor.getValue());
    Mockito.verify(metricsService)
        .getOrCreateCallbackGauge(
            Mockito.eq("hystrix.threads.cumulative.executed"),
            supplierArgumentCaptor.capture(),
            mapArgumentCaptor.capture());
    Assert.assertEquals(500l, supplierArgumentCaptor.getValue().get());
    Assert.assertEquals(dimensions, mapArgumentCaptor.getValue());
    Mockito.verify(metricsService)
        .getOrCreateCallbackGauge(
            Mockito.eq("hystrix.threads.activeThreads"),
            supplierArgumentCaptor.capture(),
            mapArgumentCaptor.capture());
    Assert.assertEquals(30, supplierArgumentCaptor.getValue().get());
    Assert.assertEquals(dimensions, mapArgumentCaptor.getValue());
    Mockito.verify(metricsService)
        .getOrCreateCallbackGauge(
            Mockito.eq("hystrix.threads.queuedTasks"),
            supplierArgumentCaptor.capture(),
            mapArgumentCaptor.capture());
    Assert.assertEquals(10, supplierArgumentCaptor.getValue().get());
    Assert.assertEquals(dimensions, mapArgumentCaptor.getValue());
    Mockito.verify(metricsService)
        .getOrCreateCallbackGauge(
            Mockito.eq("hystrix.threads.completedTasks"),
            supplierArgumentCaptor.capture(),
            mapArgumentCaptor.capture());
    Assert.assertEquals(40l, supplierArgumentCaptor.getValue().get());
    Assert.assertEquals(dimensions, mapArgumentCaptor.getValue());
  }

  @Test
  public void testGetHystrixCommandMetrics() {
    Assert.assertTrue(hystrixMetricsProcessingJob.getHystrixCommandMetrics().isEmpty());
  }

  @Test
  public void testGetHystrixThreadPoolMetrics() {
    Assert.assertTrue(hystrixMetricsProcessingJob.getHystrixThreadPoolMetrics().isEmpty());
  }
}
