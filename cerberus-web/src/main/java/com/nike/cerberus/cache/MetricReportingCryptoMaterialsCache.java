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

import com.amazonaws.encryptionsdk.caching.LocalCryptoMaterialsCache;
import com.codahale.metrics.Counter;
import com.nike.cerberus.metric.MetricsService;

public class MetricReportingCryptoMaterialsCache extends LocalCryptoMaterialsCache {
  private final Counter hitCounter;
  private final Counter missCounter;

  public MetricReportingCryptoMaterialsCache(int capacity, MetricsService metricsService) {
    super(capacity);
    hitCounter = metricsService.getOrCreateCounter("cms.cache.datakey.hit", null);
    missCounter = metricsService.getOrCreateCounter("cms.cache.datakey.miss", null);
  }

  @Override
  public EncryptCacheEntry getEntryForEncrypt(byte[] cacheId, UsageStats usageIncrement) {
    EncryptCacheEntry entryForEncrypt = super.getEntryForEncrypt(cacheId, usageIncrement);
    if (entryForEncrypt == null) {
      missCounter.inc();
    } else {
      hitCounter.inc();
    }
    return entryForEncrypt;
  }

  @Override
  public DecryptCacheEntry getEntryForDecrypt(byte[] cacheId) {
    DecryptCacheEntry entryForDecrypt = super.getEntryForDecrypt(cacheId);
    if (entryForDecrypt == null) {
      missCounter.inc();
    } else {
      hitCounter.inc();
    }
    return entryForDecrypt;
  }
}
