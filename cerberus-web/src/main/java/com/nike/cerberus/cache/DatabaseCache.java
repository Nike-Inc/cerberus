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

import static com.github.benmanes.caffeine.cache.Caffeine.newBuilder;
import static java.util.Optional.ofNullable;

import com.codahale.metrics.Counter;
import com.google.common.collect.ImmutableMap;
import com.nike.cerberus.metric.MetricsService;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.cache.Cache;

/**
 * This is a custom MyBatis Cache, that allows use to do the following 1. Report cache statistics
 * via Dropwizard 2. Expire items automatically after some TTL from when they were cached. (To avoid
 * needing to deal with distributed cache busting, this basically makes cached data eventually
 * consistent up to the defined TTL) 3. Only cache items after it has been proven via repeat reads
 * that they should be cached. (To avoid unnecessary eventual consistency in the dashboard, only
 * make the items under heavy reads eventually consistent) See cms.conf for all the configuration
 * settings.
 */
@Slf4j
public class DatabaseCache implements Cache {

  protected final Integer repeatReadThreshold;
  protected final String id;
  protected MetricReportingCache<Object, Object> dataCache;
  protected com.github.benmanes.caffeine.cache.Cache<Object, Counter>
      autoExpiringRepeatReadCounterMap;

  public DatabaseCache(
      String id,
      MetricsService metricsService,
      int dataTtlInSeconds,
      int repeatReadCounterResetInSeconds,
      int repeatReadThreshold) {

    this.id = id;
    this.repeatReadThreshold = repeatReadThreshold;

    log.info(
        "Database cache created with id: {}, dataTtlInSeconds: {}, repeatReadCounterResetInSeconds: {}, repeatReadThreshold: {}",
        id,
        dataTtlInSeconds,
        repeatReadCounterResetInSeconds,
        repeatReadThreshold);

    dataCache =
        new MetricReportingCache<>(
            "mybatis", dataTtlInSeconds, metricsService, ImmutableMap.of("namespace", this.id));

    autoExpiringRepeatReadCounterMap =
        newBuilder().expireAfterAccess(repeatReadCounterResetInSeconds, TimeUnit.SECONDS).build();
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void putObject(Object key, Object value) {
    if (key == null || value == null) {
      return;
    }

    // If the read counter exists and is greater than the threshold then we are receiving
    // burst repeat reads and we will cache that entry.
    ofNullable(autoExpiringRepeatReadCounterMap.getIfPresent(key))
        .ifPresent(
            counter -> {
              if (counter.getCount() > repeatReadThreshold) {
                dataCache.put(key, value);
              }
            });
  }

  @Override
  public Object getObject(Object key) {
    // Increment the read counter, which resets after counterExpireTimeInSeconds.
    Counter counter = autoExpiringRepeatReadCounterMap.getIfPresent(key);
    if (counter != null) {
      counter.inc();
    } else {
      counter = new Counter();
      counter.inc();
      autoExpiringRepeatReadCounterMap.put(key, counter);
    }

    return dataCache.getIfPresent(key);
  }

  @Override
  public Object removeObject(Object key) {
    Object res = dataCache.getIfPresent(key);
    dataCache.invalidate(key);
    return res;
  }

  @Override
  public void clear() {
    // NO-OP, my batis by default clears the entire namespaced cache when a write action occurs,
    // we do not want that here, we are expiring the cache / making reads eventually consistent.
    // Since we run Cerberus in a cluster anyways and each instance will have it's own generated
    // cache, a simple small
    // time window where items purge themselves is adequate.
  }

  @Override
  public int getSize() {
    try {
      return Math.toIntExact(dataCache.estimatedSize());
    } catch (ArithmeticException e) {
      return Integer.MAX_VALUE;
    }
  }

  @Override
  public ReadWriteLock getReadWriteLock() {
    return null;
  }

  @Override
  public int hashCode() {
    return Objects.hash(dataCache, getId());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof DatabaseCache)) return false;
    DatabaseCache that = (DatabaseCache) o;
    return dataCache.equals(that.dataCache) && getId().equals(that.getId());
  }
}
