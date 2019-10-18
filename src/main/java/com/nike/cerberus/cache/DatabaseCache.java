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
 *
 */

package com.nike.cerberus.cache;

import com.codahale.metrics.Counter;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Injector;
import com.nike.cerberus.server.config.guice.StaticInjector;
import com.nike.cerberus.service.MetricsService;
import com.typesafe.config.Config;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.builder.InitializingObject;
import org.apache.ibatis.cache.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;

import static com.github.benmanes.caffeine.cache.Caffeine.newBuilder;
import static java.util.Optional.ofNullable;

/**
 * TODO rewrite
 * A simple Caffeine backed cache that auto expires items after a certain time period,
 * to help us against bursty traffic that does repeat reads.
 */
public class DatabaseCache implements Cache, InitializingObject {

  private final Logger log = LoggerFactory.getLogger(getClass());
  protected final String id;
  private MetricsService metricsService;

  protected static final String GLOBAL_DATA_TTL_IN_SECONDS = "cms.mybatis.cache.global.dataTtlInSeconds";
  protected static final String DATA_TTL_IN_SECONDS_OVERRIDE_PATH_TEMPLATE = "cms.mybatis.cache.%s.dataTtlInSeconds";
  protected static final String GLOBAL_REPEAT_READ_COUNTER_EXPIRE_IN_SECONDS = "cms.mybatis.cache.global.repeatReadCounterResetInSeconds";
  protected static final String REPEAT_READ_COUNTER_EXPIRE_IN_SECONDS_OVERRIDE_PATH_TEMPLATE = "cms.mybatis.cache.%s.repeatReadCounterResetInSeconds";
  protected static final String GLOBAL_REPEAT_READ_THRESHOLD = "cms.mybatis.cache.global.repeatReadThreshold";
  protected static final String REPEAT_READ_THRESHOLD_OVERRIDE_PATH_TEMPLATE = "cms.mybatis.cache.%s.repeatReadThreshold";
  protected static final int DEFAULT_GLOBAL_DATA_TTL_IN_SECONDS = 10;
  protected static final int DEFAULT_REPEAT_READ_COUNTER_EXPIRE_IN_SECONDS = 1;
  protected static final int DEFAULT_REPEAT_READ_THRESHOLD = 5;

  protected com.github.benmanes.caffeine.cache.Cache<Object, Object> dataCache;
  protected com.github.benmanes.caffeine.cache.Cache<Object, Counter> autoExpiringRepeatReadCounterMap;
  protected Integer repeatReadThreshold;
  protected Counter hitCounter;
  protected Counter missCounter;

  public DatabaseCache(String id) {
    this.id = id;
  }

  /**
   * This method gets called after this class is instantiated by MyBatis and all the properties have been set.
   */
  @Override
  public void initialize() {
    // Util we can get the MyBatis Guice Module updated, this is our best bet, for getting the Guice instances.
    // https://groups.google.com/forum/#!msg/mybatis-user/Ekd1LTNVIDc/t2xGuvETBgAJ
    Injector injector = StaticInjector.getInstance();

    Config config = injector.getInstance(Config.class);
    metricsService = injector.getInstance(MetricsService.class);

    String mapperKey = StringUtils.uncapitalize(id.replaceFirst("com.nike.cerberus.mapper.", ""));
    int expireTimeInSeconds = getExpireTimeInSeconds(config, mapperKey);
    int counterExpireTimeInSeconds = getRepeatReadCounterExpireTimeInSeconds(config, mapperKey);
    repeatReadThreshold = getRepeatReadThreshold(config, mapperKey);

    log.info("Database cache created with mapperKey: {}, expireTimeInSeconds: {}, counterExpireTimeInSeconds: {}, repeatReadThreshold: {}",
        mapperKey, expireTimeInSeconds, counterExpireTimeInSeconds, repeatReadThreshold);

    dataCache = newBuilder()
        .expireAfterWrite(expireTimeInSeconds, TimeUnit.SECONDS)
        .build();

    autoExpiringRepeatReadCounterMap = newBuilder()
        .expireAfterAccess(counterExpireTimeInSeconds, TimeUnit.SECONDS)
        .build();

    instrumentCacheMetrics();
  }

  /**
   * Instrument metrics about this database cache for monitoring, alerting, canary, etc.
   */
  private void instrumentCacheMetrics() {
    Map<String, String> dimensions = ImmutableMap.of("namespace", this.id);
    // Create Metrics for this cache for observability.
    hitCounter = metricsService.getOrCreateCounter("cms.cache.mybatis.hit", dimensions);
    missCounter = metricsService.getOrCreateCounter("cms.cache.mybatis.miss", dimensions);
    metricsService.getOrCreateLongCallbackGauge("cms.cache.mybatis.size",
        () -> dataCache.estimatedSize(), dimensions);
    metricsService.getOrCreateLongCallbackGauge("cms.cache.mybatis.stats.totalHitCount",
        () -> dataCache.stats().hitCount(), dimensions);
    metricsService.getOrCreateLongCallbackGauge("cms.cache.mybatis.stats.totalMissCount",
        () -> dataCache.stats().missCount(), dimensions);
  }

  /**
   * @param config The application config
   * @param mapperKey The key for this mapper
   * @return The amount of time in seconds that the mapper cache will keep an item in memory before it purges itself.
   */
  protected int getExpireTimeInSeconds(Config config, String mapperKey) {
    int globalExpireTimeInSeconds = config.hasPath(GLOBAL_DATA_TTL_IN_SECONDS) ? config.getInt(GLOBAL_DATA_TTL_IN_SECONDS) : DEFAULT_GLOBAL_DATA_TTL_IN_SECONDS;
    String globalDataTtlInSecondsOverridePathTemplate = String.format(DATA_TTL_IN_SECONDS_OVERRIDE_PATH_TEMPLATE, mapperKey);
    return config.hasPath(globalDataTtlInSecondsOverridePathTemplate) ? config.getInt(globalDataTtlInSecondsOverridePathTemplate) : globalExpireTimeInSeconds;
  }

  /**
   * @param config The application config
   * @param mapperKey The key for this mapper
   * @return The amount of time in seconds that must pass without consecutive reads to reset the counter.
   */
  protected int getRepeatReadCounterExpireTimeInSeconds(Config config, String mapperKey) {
    int globalCounterExpireTimeInSeconds = config.hasPath(GLOBAL_REPEAT_READ_COUNTER_EXPIRE_IN_SECONDS) ? config.getInt(GLOBAL_REPEAT_READ_COUNTER_EXPIRE_IN_SECONDS) : DEFAULT_REPEAT_READ_COUNTER_EXPIRE_IN_SECONDS;
    String counterMapperOverrideTtlPath = String.format(REPEAT_READ_COUNTER_EXPIRE_IN_SECONDS_OVERRIDE_PATH_TEMPLATE, mapperKey);
    return config.hasPath(counterMapperOverrideTtlPath) ? config.getInt(counterMapperOverrideTtlPath) : globalCounterExpireTimeInSeconds;
  }

  /**
   * @param config The application config
   * @param mapperKey The key for this mapper
   * @return The number of reads that must be exceeding while counts are being chained before caching of that object is enabled.
   */
  protected int getRepeatReadThreshold(Config config, String mapperKey) {
    int globalRepeatReadThreshold = config.hasPath(GLOBAL_REPEAT_READ_THRESHOLD) ? config.getInt(GLOBAL_REPEAT_READ_THRESHOLD) : DEFAULT_REPEAT_READ_THRESHOLD;
    String repeatReadThresholdOverridePath = String.format(REPEAT_READ_THRESHOLD_OVERRIDE_PATH_TEMPLATE, mapperKey);
    return config.hasPath(repeatReadThresholdOverridePath) ? config.getInt(repeatReadThresholdOverridePath) : globalRepeatReadThreshold;
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
    ofNullable(autoExpiringRepeatReadCounterMap.getIfPresent(key)).ifPresent(counter -> {
      if (counter.getCount() > repeatReadThreshold) {
        dataCache.put(key, value);
      }
    });
  }

  @Override
  public Object getObject(Object key) {
    Object value = dataCache.getIfPresent(key);
    if (value == null) {
      missCounter.inc();
    } else {
      hitCounter.inc();
    }

    // Increment the read counter, which resets after counterExpireTimeInSeconds.
    Counter counter = autoExpiringRepeatReadCounterMap.getIfPresent(key);
    if (counter != null) {
      counter.inc();
    } else {
      counter = new Counter();
      counter.inc();
      autoExpiringRepeatReadCounterMap.put(key, counter);
    }
    return value;
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
    // Since we run Cerberus in a cluster anyways and each instance will have it's own generated cache, a simple small
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
    return dataCache.equals(that.dataCache) &&
        getId().equals(that.getId());
  }
}
