package com.nike.cerberus.cache;

import com.codahale.metrics.Counter;
import com.google.common.collect.ImmutableMap;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.nike.cerberus.server.config.guice.StaticInjector;
import com.nike.cerberus.service.MetricsService;
import com.typesafe.config.Config;
import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.time.Duration;
import java.util.Objects;

import static com.nike.cerberus.cache.DatabaseCache.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class DatabaseCacheTest {

  private String mapperKey = "myMapper";

  @Mock
  private Config config;

  @Mock
  private MetricsService metricsService;

  private DatabaseCache databaseCache;

  @Before
  public void before() {
    initMocks(this);
    Guice.createInjector(new AbstractModule() {
      @Override
      protected void configure() {
        requestStaticInjection(StaticInjector.class);
        bind(Config.class).toInstance(config);
        bind(MetricsService.class).toInstance(metricsService);
      }
    });

    databaseCache = new DatabaseCache("com.nike.cerberus.mapper." + mapperKey);
  }

  @Test
  public void test_that_getExpireTimeInSeconds_returns_the_default_setting_when_no_props_are_provided() {
    String globalDataTtlInSecondsOverridePathTemplate = String.format(DATA_TTL_IN_SECONDS_OVERRIDE_PATH_TEMPLATE, mapperKey);
    when(config.hasPath(GLOBAL_DATA_TTL_IN_SECONDS)).thenReturn(false);
    when(config.hasPath(globalDataTtlInSecondsOverridePathTemplate)).thenReturn(false);
    assertEquals(DEFAULT_GLOBAL_DATA_TTL_IN_SECONDS, databaseCache.getExpireTimeInSeconds(config, mapperKey));
  }

  @Test
  public void test_that_getExpireTimeInSeconds_returns_the_expected_result_when_a_global_override_is_provided() {
    String globalDataTtlInSecondsOverridePathTemplate = String.format(DATA_TTL_IN_SECONDS_OVERRIDE_PATH_TEMPLATE, mapperKey);
    when(config.hasPath(GLOBAL_DATA_TTL_IN_SECONDS)).thenReturn(true);
    when(config.getInt(GLOBAL_DATA_TTL_IN_SECONDS)).thenReturn(2);
    when(config.hasPath(globalDataTtlInSecondsOverridePathTemplate)).thenReturn(false);
    assertEquals(2, databaseCache.getExpireTimeInSeconds(config, mapperKey));
  }

  @Test
  public void test_that_getExpireTimeInSeconds_returns_the_expected_result_when_a_global_and_mapper_override_is_provided() {
    String globalDataTtlInSecondsOverridePathTemplate = String.format(DATA_TTL_IN_SECONDS_OVERRIDE_PATH_TEMPLATE, mapperKey);
    when(config.hasPath(GLOBAL_DATA_TTL_IN_SECONDS)).thenReturn(true);
    when(config.getInt(GLOBAL_DATA_TTL_IN_SECONDS)).thenReturn(2);
    when(config.hasPath(globalDataTtlInSecondsOverridePathTemplate)).thenReturn(true);
    when(config.getInt(globalDataTtlInSecondsOverridePathTemplate)).thenReturn(5);
    assertEquals(5, databaseCache.getExpireTimeInSeconds(config, mapperKey));
  }

  @Test
  public void test_that_getExpireTimeInSeconds_returns_the_expected_result_when_a_mapper_override_is_provided() {
    String globalDataTtlInSecondsOverridePathTemplate = String.format(DATA_TTL_IN_SECONDS_OVERRIDE_PATH_TEMPLATE, mapperKey);
    when(config.hasPath(GLOBAL_DATA_TTL_IN_SECONDS)).thenReturn(false);
    when(config.hasPath(globalDataTtlInSecondsOverridePathTemplate)).thenReturn(true);
    when(config.getInt(globalDataTtlInSecondsOverridePathTemplate)).thenReturn(5);
    assertEquals(5, databaseCache.getExpireTimeInSeconds(config, mapperKey));
  }

  @Test
  public void test_that_getCounterExpireTimeInSeconds_returns_the_default_setting_when_no_props_are_provided() {
    String repeatReadCounterExpireInSecondsPathTemplate = String.format(REPEAT_READ_COUNTER_EXPIRE_IN_SECONDS_OVERRIDE_PATH_TEMPLATE, mapperKey);
    when(config.hasPath(GLOBAL_REPEAT_READ_COUNTER_EXPIRE_IN_SECONDS)).thenReturn(false);
    when(config.hasPath(repeatReadCounterExpireInSecondsPathTemplate)).thenReturn(false);
    assertEquals(DEFAULT_REPEAT_READ_COUNTER_EXPIRE_IN_SECONDS, databaseCache.getRepeatReadCounterExpireTimeInSeconds(config, mapperKey));
  }

  @Test
  public void test_that_getCounterExpireTimeInSeconds_returns_the_expected_result_when_a_global_override_is_provided() {
    String repeatReadCounterExpireInSecondsPathTemplate = String.format(REPEAT_READ_COUNTER_EXPIRE_IN_SECONDS_OVERRIDE_PATH_TEMPLATE, mapperKey);
    when(config.hasPath(GLOBAL_REPEAT_READ_COUNTER_EXPIRE_IN_SECONDS)).thenReturn(true);
    when(config.getInt(GLOBAL_REPEAT_READ_COUNTER_EXPIRE_IN_SECONDS)).thenReturn(33);
    when(config.hasPath(repeatReadCounterExpireInSecondsPathTemplate)).thenReturn(false);
    assertEquals(33, databaseCache.getRepeatReadCounterExpireTimeInSeconds(config, mapperKey));
  }

  @Test
  public void test_that_getCounterExpireTimeInSeconds_returns_the_expected_result_when_a_global_and_mapper_override_is_provided() {
    String repeatReadCounterExpireInSecondsPathTemplate = String.format(REPEAT_READ_COUNTER_EXPIRE_IN_SECONDS_OVERRIDE_PATH_TEMPLATE, mapperKey);
    when(config.hasPath(GLOBAL_REPEAT_READ_COUNTER_EXPIRE_IN_SECONDS)).thenReturn(true);
    when(config.getInt(GLOBAL_REPEAT_READ_COUNTER_EXPIRE_IN_SECONDS)).thenReturn(2);
    when(config.hasPath(repeatReadCounterExpireInSecondsPathTemplate)).thenReturn(true);
    when(config.getInt(repeatReadCounterExpireInSecondsPathTemplate)).thenReturn(5);
    assertEquals(5, databaseCache.getRepeatReadCounterExpireTimeInSeconds(config, mapperKey));
  }

  @Test
  public void test_that_getCounterExpireTimeInSeconds_returns_the_expected_result_when_a_mapper_override_is_provided() {
    String repeatReadCounterExpireInSecondsPathTemplate = String.format(REPEAT_READ_COUNTER_EXPIRE_IN_SECONDS_OVERRIDE_PATH_TEMPLATE, mapperKey);
    when(config.hasPath(GLOBAL_REPEAT_READ_COUNTER_EXPIRE_IN_SECONDS)).thenReturn(false);
    when(config.hasPath(repeatReadCounterExpireInSecondsPathTemplate)).thenReturn(true);
    when(config.getInt(repeatReadCounterExpireInSecondsPathTemplate)).thenReturn(5);
    assertEquals(5, databaseCache.getRepeatReadCounterExpireTimeInSeconds(config, mapperKey));
  }

  @Test
  public void test_that_getRepeatReadThreshold_returns_the_default_setting_when_no_props_are_provided() {
    String globalRepeatReadThreshold = String.format(REPEAT_READ_THRESHOLD_OVERRIDE_PATH_TEMPLATE, mapperKey);
    when(config.hasPath(GLOBAL_REPEAT_READ_THRESHOLD)).thenReturn(false);
    when(config.hasPath(globalRepeatReadThreshold)).thenReturn(false);
    assertEquals(DEFAULT_REPEAT_READ_THRESHOLD, databaseCache.getRepeatReadThreshold(config, mapperKey));
  }

  @Test
  public void test_that_getRepeatReadThreshold_returns_the_expected_result_when_a_global_override_is_provided() {
    String globalRepeatReadThreshold = String.format(REPEAT_READ_THRESHOLD_OVERRIDE_PATH_TEMPLATE, mapperKey);
    when(config.hasPath(GLOBAL_REPEAT_READ_THRESHOLD)).thenReturn(true);
    when(config.getInt(GLOBAL_REPEAT_READ_THRESHOLD)).thenReturn(33);
    when(config.hasPath(globalRepeatReadThreshold)).thenReturn(false);
    assertEquals(33, databaseCache.getRepeatReadThreshold(config, mapperKey));
  }

  @Test
  public void test_that_getRepeatReadThreshold_returns_the_expected_result_when_a_global_and_mapper_override_is_provided() {
    String globalRepeatReadThreshold = String.format(REPEAT_READ_THRESHOLD_OVERRIDE_PATH_TEMPLATE, mapperKey);
    when(config.hasPath(GLOBAL_REPEAT_READ_THRESHOLD)).thenReturn(true);
    when(config.getInt(GLOBAL_REPEAT_READ_THRESHOLD)).thenReturn(2);
    when(config.hasPath(globalRepeatReadThreshold)).thenReturn(true);
    when(config.getInt(globalRepeatReadThreshold)).thenReturn(5);
    assertEquals(5, databaseCache.getRepeatReadThreshold(config, mapperKey));
  }

  @Test
  public void test_that_getRepeatReadThreshold_returns_the_expected_result_when_a_mapper_override_is_provided() {
    String globalRepeatReadThreshold = String.format(REPEAT_READ_THRESHOLD_OVERRIDE_PATH_TEMPLATE, mapperKey);
    when(config.hasPath(GLOBAL_REPEAT_READ_THRESHOLD)).thenReturn(false);
    when(config.hasPath(globalRepeatReadThreshold)).thenReturn(true);
    when(config.getInt(globalRepeatReadThreshold)).thenReturn(5);
    assertEquals(5, databaseCache.getRepeatReadThreshold(config, mapperKey));
  }

  @Test
  public void test_that_equals_returns_true_when_two_caches_are_equal() {
    TestCase.assertEquals(databaseCache, databaseCache);
  }

  @Test
  public void test_that_the_cache_behaves_as_expected_when_simulated_db_reads_happen_at_various_rates() throws InterruptedException {
    String key = "SELECT foo FROM bar WHERE id = '123-abc'";
    String value = "The secret to life is 43.";
    String aSecondValue = "LoL > Dota. QED";
    int expireTimeInSeconds = 2;
    int repeatReadCounterExpireTimeInSeconds = 1;
    int repeatReadThreshold = 1;
    int chainedReadCount = 0;

    // configure and init the cache
    when(config.hasPath(GLOBAL_DATA_TTL_IN_SECONDS)).thenReturn(true);
    when(config.getInt(GLOBAL_DATA_TTL_IN_SECONDS)).thenReturn(expireTimeInSeconds);
    when(config.hasPath(GLOBAL_REPEAT_READ_COUNTER_EXPIRE_IN_SECONDS)).thenReturn(true);
    when(config.getInt(GLOBAL_REPEAT_READ_COUNTER_EXPIRE_IN_SECONDS)).thenReturn(repeatReadCounterExpireTimeInSeconds);
    when(config.hasPath(GLOBAL_REPEAT_READ_THRESHOLD)).thenReturn(true);
    when(config.getInt(GLOBAL_REPEAT_READ_THRESHOLD)).thenReturn(repeatReadThreshold);
    when(metricsService.getOrCreateCounter("cms.cache.mybatis.miss", ImmutableMap.of("namespace", databaseCache.id))).thenReturn(new Counter());
    Counter hitCounter = new Counter();
    when(metricsService.getOrCreateCounter("cms.cache.mybatis.hit", ImmutableMap.of("namespace", databaseCache.id))).thenReturn(hitCounter);
    databaseCache.initialize();

    // Simulate the first db read, because the read count hasn't passed the threshold it shouldn't cache.
    assertNull("The first get call to the cache should return null",
        databaseCache.getObject(key));
    chainedReadCount++;
    assertEquals("The miss counter should have been incremented", 1, databaseCache.missCounter.getCount());
    assertEquals("The hit counter should have  been incremented", 0, databaseCache.hitCounter.getCount());
    assertEquals("The repeat read counter for the key should increment", chainedReadCount,
        Objects.requireNonNull(databaseCache.autoExpiringRepeatReadCounterMap.getIfPresent(key)).getCount());
    databaseCache.putObject(key, value);
    assertEquals("The cache should be empty for the first put because the count is lower than the threshold",
        0, databaseCache.dataCache.estimatedSize());

    // Simulate the second db read in < 1 second (this should trigger a cache 2 reads without a 1 second break > 1)
    assertNull("The second get call to the cache should return null, because the 1st didn't exceed the threshold",
        databaseCache.getObject(key));
    chainedReadCount++;
    assertEquals("The miss counter should have been incremented", 2, databaseCache.missCounter.getCount());
    assertEquals("The hit counter should have  been incremented", 0, databaseCache.hitCounter.getCount());
    assertEquals("The repeat read counter for the key should increment", chainedReadCount,
        Objects.requireNonNull(databaseCache.autoExpiringRepeatReadCounterMap.getIfPresent(key)).getCount());
    databaseCache.putObject(key, value);
    assertEquals("The cache should now have an item since the threshold was exceeded",
        1, databaseCache.dataCache.estimatedSize());

    // Simulate the third db read in < 1 second this should fetch the cached result and keep the increment the repeat read counter
    assertEquals("The third get call to the cache should return the cached value.",
        value, databaseCache.getObject(key));
    chainedReadCount++;
    assertEquals("The miss counter should have not been incremented", 2, databaseCache.missCounter.getCount());
    assertEquals("The hit counter should have  been incremented", 1, databaseCache.hitCounter.getCount());
    assertEquals("The repeat read counter for the key should increment", chainedReadCount,
        Objects.requireNonNull(databaseCache.autoExpiringRepeatReadCounterMap.getIfPresent(key)).getCount());

    // Simulate reads for expireTimeInSeconds + 1 second
    int i = 0;
    while (i < Duration.ofSeconds(expireTimeInSeconds + 1).toMillis()) {
      databaseCache.getObject(key); // This should become null as some point in this loop, from it expiring
      assertEquals("Expected the read counter to equal the chained read count", ++chainedReadCount, Objects.requireNonNull(databaseCache.autoExpiringRepeatReadCounterMap.getIfPresent(key)).getCount());
      Thread.sleep(250);
      i += 250;
    }

    // Simulate the new db read after the value was purged but while the chained counter is still > the threshold. Caching should occur for new value
    assertNull("The key/value should have purged itself from the cache when we looped for longer than the ttl, simulating reads", databaseCache.getObject(key));
    databaseCache.putObject(key, aSecondValue);
    assertEquals("The updated value should have been immediately available in the cache", aSecondValue, databaseCache.getObject(key));

    // now simulate no activity for expireTimeInSeconds + 1 second
    Thread.sleep(Duration.ofSeconds(expireTimeInSeconds + 1).toMillis());
    assertNull("The repeat read counter for the key should be purged from the map from idle activity", databaseCache.autoExpiringRepeatReadCounterMap.getIfPresent(key));
    assertNull("The key/value should have purged itself from the cache from idle activity", databaseCache.getObject(key));
  }
}
