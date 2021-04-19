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

package com.nike.cerberus.cache;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.codahale.metrics.Counter;
import com.google.common.collect.ImmutableMap;
import com.nike.cerberus.metric.MetricsService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Duration;
import java.util.Objects;
import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class DatabaseCacheTest {

  @Mock private MetricsService metricsService;

  @Before
  public void before() {
    initMocks(this);
  }

  @Test
  public void test_that_equals_returns_true_when_two_caches_are_equal() {
    DatabaseCache databaseCache = new DatabaseCache("it", metricsService, 1, 1, 1);
    TestCase.assertEquals(databaseCache, databaseCache);
  }

  @Test
  @SuppressFBWarnings
  public void
      test_that_the_cache_behaves_as_expected_when_simulated_db_reads_happen_at_various_rates()
          throws InterruptedException {
    String key = "SELECT foo FROM bar WHERE id = '123-abc'";
    String value = "The secret to life is 43.";
    String aSecondValue = "LoL > Dota. QED";
    int expireTimeInSeconds = 2;
    int repeatReadCounterExpireTimeInSeconds = 1;
    int repeatReadThreshold = 1;
    int chainedReadCount = 0;

    var id = "test-id";
    when(metricsService.getOrCreateCounter(
            "cms.cache.mybatis.miss", ImmutableMap.of("namespace", id)))
        .thenReturn(new Counter());
    Counter hitCounter = new Counter();
    when(metricsService.getOrCreateCounter(
            "cms.cache.mybatis.hit", ImmutableMap.of("namespace", id)))
        .thenReturn(hitCounter);
    DatabaseCache databaseCache =
        new DatabaseCache(
            id,
            metricsService,
            expireTimeInSeconds,
            repeatReadCounterExpireTimeInSeconds,
            repeatReadThreshold);

    // Simulate the first db read, because the read count hasn't passed the threshold it shouldn't
    // cache.
    assertNull("The first get call to the cache should return null", databaseCache.getObject(key));
    chainedReadCount++;
    assertEquals(
        "The miss counter should have been incremented",
        1,
        databaseCache.dataCache.getMissCounter().getCount());
    assertEquals(
        "The hit counter should have  been incremented",
        0,
        databaseCache.dataCache.getHitCounter().getCount());
    assertEquals(
        "The repeat read counter for the key should increment",
        chainedReadCount,
        Objects.requireNonNull(databaseCache.autoExpiringRepeatReadCounterMap.getIfPresent(key))
            .getCount());
    databaseCache.putObject(key, value);
    assertEquals(
        "The cache should be empty for the first put because the count is lower than the threshold",
        0,
        databaseCache.dataCache.estimatedSize());

    // Simulate the second db read in < 1 second (this should trigger a cache 2 reads without a 1
    // second break > 1)
    assertNull(
        "The second get call to the cache should return null, because the 1st didn't exceed the threshold",
        databaseCache.getObject(key));
    chainedReadCount++;
    assertEquals(
        "The miss counter should have been incremented",
        2,
        databaseCache.dataCache.getMissCounter().getCount());
    assertEquals(
        "The hit counter should have  been incremented",
        0,
        databaseCache.dataCache.getHitCounter().getCount());
    assertEquals(
        "The repeat read counter for the key should increment",
        chainedReadCount,
        Objects.requireNonNull(databaseCache.autoExpiringRepeatReadCounterMap.getIfPresent(key))
            .getCount());
    databaseCache.putObject(key, value);
    assertEquals(
        "The cache should now have an item since the threshold was exceeded",
        1,
        databaseCache.dataCache.estimatedSize());

    // Simulate the third db read in < 1 second this should fetch the cached result and keep the
    // increment the repeat read counter
    assertEquals(
        "The third get call to the cache should return the cached value.",
        value,
        databaseCache.getObject(key));
    chainedReadCount++;
    assertEquals(
        "The miss counter should have not been incremented",
        2,
        databaseCache.dataCache.getMissCounter().getCount());
    assertEquals(
        "The hit counter should have  been incremented",
        1,
        databaseCache.dataCache.getHitCounter().getCount());
    assertEquals(
        "The repeat read counter for the key should increment",
        chainedReadCount,
        Objects.requireNonNull(databaseCache.autoExpiringRepeatReadCounterMap.getIfPresent(key))
            .getCount());

    // Simulate reads for expireTimeInSeconds + 1 second
    int i = 0;
    while (i < Duration.ofSeconds(expireTimeInSeconds + 1).toMillis()) {
      databaseCache.getObject(
          key); // This should become null as some point in this loop, from it expiring
      assertEquals(
          "Expected the read counter to equal the chained read count",
          ++chainedReadCount,
          Objects.requireNonNull(databaseCache.autoExpiringRepeatReadCounterMap.getIfPresent(key))
              .getCount());
      Thread.sleep(250);
      i += 250;
    }

    // Simulate the new db read after the value was purged but while the chained counter is still >
    // the threshold. Caching should occur for new value
    assertNull(
        "The key/value should have purged itself from the cache when we looped for longer than the ttl, simulating reads",
        databaseCache.getObject(key));
    databaseCache.putObject(key, aSecondValue);
    assertEquals(
        "The updated value should have been immediately available in the cache",
        aSecondValue,
        databaseCache.getObject(key));

    // now simulate no activity for expireTimeInSeconds + 1 second
    Thread.sleep(Duration.ofSeconds(expireTimeInSeconds + 1).toMillis());
    assertNull(
        "The repeat read counter for the key should be purged from the map from idle activity",
        databaseCache.autoExpiringRepeatReadCounterMap.getIfPresent(key));
    assertNull(
        "The key/value should have purged itself from the cache from idle activity",
        databaseCache.getObject(key));
  }
}
