package com.nike.cerberus.cache;

import com.amazonaws.encryptionsdk.caching.LocalCryptoMaterialsCache;
import com.codahale.metrics.Counter;
import com.nike.cerberus.service.MetricsService;

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
