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
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Policy;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import com.nike.cerberus.service.MetricsService;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.github.benmanes.caffeine.cache.Caffeine.newBuilder;

/**
 * A simple Caffeine backed cache that auto expires items after a certain time period,
 * to help us against bursty traffic that does repeat reads.
 */
public class MetricReportingCache<K, V> implements Cache<K, V> {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private Cache<K, V> delegate;
    private final Counter hitCounter;
    private final Counter missCounter;

    public MetricReportingCache(String namespace, int expireTimeInSeconds, MetricsService metricsService,
                                Map<String, String> dimensions) {
        log.info("Cerberus cache with namespace: {} has been initialized with ttl: {}", namespace, expireTimeInSeconds);

        delegate = newBuilder()
                .expireAfterWrite(expireTimeInSeconds, TimeUnit.SECONDS)
                .build();

        // Create Metrics for this cache.
        hitCounter = metricsService.getOrCreateCounter(String.format("cms.cache.%s.hit", namespace), dimensions);
        missCounter = metricsService.getOrCreateCounter(String.format("cms.cache.%s.miss", namespace), dimensions);
        metricsService.getOrCreateLongCallbackGauge(String.format("cms.cache.%s.size", namespace),
                () -> delegate.estimatedSize(), dimensions);
        metricsService.getOrCreateLongCallbackGauge(String.format("cms.cache.%s.stats.totalHitCount", namespace),
                () -> delegate.stats().hitCount(), dimensions);
        metricsService.getOrCreateLongCallbackGauge(String.format("cms.cache.%s.stats.totalMissCount", namespace),
                () -> delegate.stats().missCount(), dimensions);
    }

    @Override
    public V getIfPresent(Object key) {
        V value = delegate.getIfPresent(key);
        if (value == null) {
            missCounter.inc();
        } else {
            hitCounter.inc();
        }
        return value;
    }

    @Override
    public V get(K key, Function<? super K, ? extends V> mappingFunction) {
        V value = delegate.getIfPresent(key);
        if (value == null) {
            missCounter.inc();
            return delegate.get(key, mappingFunction);
        } else {
            hitCounter.inc();
            return value;
        }
    }

    @Override
    public void put(K key, V value) {
        delegate.put(key, value);
    }

    @Override
    public void putAll(Map map) {
        delegate.putAll(map);
    }

    @Override
    public void invalidate(Object key) {
        delegate.invalidate(key);
    }

    @Override
    public void invalidateAll() {
        delegate.invalidateAll();
    }

    @Override
    public long estimatedSize() {
        return delegate.estimatedSize();
    }

    @Override
    public CacheStats stats() {
        return delegate.stats();
    }

    @Override
    public ConcurrentMap asMap() {
        return delegate.asMap();
    }

    @Override
    public void cleanUp() {
        delegate.cleanUp();
    }

    @Override
    public Policy policy() {
        return delegate.policy();
    }

    @Override
    public void invalidateAll(Iterable keys) {

    }

    @Override
    public @NonNull Map getAllPresent(Iterable keys) {
        return delegate.getAllPresent(keys);
    }

    public Counter getHitCounter() {
        return hitCounter;
    }

    public Counter getMissCounter() {
        return missCounter;
    }
}
