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
import com.nike.cerberus.service.ConfigService;
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

/**
 * A simple Caffeine backed cache that auto expires items after a certain time period,
 * to help us against bursty traffic that does repeat reads.
 */
public class DatabaseCache implements Cache, InitializingObject {

  private final Logger log = LoggerFactory.getLogger(getClass());
  private final String GLOBAL_TTL = "cms.mybatis.cache.global.TtlInSeconds";
  private final int DEFAULT_TTL = 10;
  private final String id;

  private com.github.benmanes.caffeine.cache.Cache<Object, Object> delegate;
  private Counter hitCounter;
  private Counter missCounter;

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
    Config config = ConfigService.getInstance().getAppConfigMergedWithCliGeneratedProperties();

    MetricsService metricsService = injector.getInstance(MetricsService.class);

    int globalExpireTimeInSeconds = config.hasPath(GLOBAL_TTL) ? config.getInt(GLOBAL_TTL) : DEFAULT_TTL;
    String mapperOverrideTtlPath = "cms.mybatis.cache." + StringUtils.uncapitalize(id.replaceFirst("com.nike.cerberus.mapper.", "")) + ".ttlInSeconds";
    int expireTimeInSeconds = config.hasPath(mapperOverrideTtlPath) ? config.getInt(mapperOverrideTtlPath) : globalExpireTimeInSeconds;

    log.info("Database cache with namespace: {} has been initialized with ttl: {}", id, expireTimeInSeconds);

    delegate = newBuilder()
        .expireAfterWrite(expireTimeInSeconds, TimeUnit.SECONDS)
        .build();

    Map<String, String> dimensions = ImmutableMap.of("namespace", id);

    // Create Metrics for this cache.
    hitCounter = metricsService.getOrCreateCounter("cms.cache.mybatis.hit", dimensions);
    missCounter = metricsService.getOrCreateCounter("cms.cache.mybatis.miss", dimensions);
    metricsService.getOrCreateLongCallbackGauge("cms.cache.mybatis.size",
        () -> delegate.estimatedSize(), dimensions);
    metricsService.getOrCreateLongCallbackGauge("cms.cache.mybatis.stats.totalHitCount",
        () -> delegate.stats().hitCount(), dimensions);
    metricsService.getOrCreateLongCallbackGauge("cms.cache.mybatis.stats.totalMissCount",
        () -> delegate.stats().missCount(), dimensions);
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
    delegate.put(key, value);
  }

  @Override
  public Object getObject(Object key) {
    Object value = delegate.getIfPresent(key);
    if (value == null) {
      missCounter.inc();
    } else {
      hitCounter.inc();
    }
    return value;
  }

  @Override
  public Object removeObject(Object key) {
    Object res = delegate.getIfPresent(key);
    delegate.invalidate(key);
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
      return Math.toIntExact(delegate.estimatedSize());
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
    return Objects.hash(delegate, getId());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof DatabaseCache)) return false;
    DatabaseCache that = (DatabaseCache) o;
    return delegate.equals(that.delegate) &&
        getId().equals(that.getId());
  }
}
