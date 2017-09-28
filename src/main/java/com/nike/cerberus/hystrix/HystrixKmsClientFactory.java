package com.nike.cerberus.hystrix;

import com.amazonaws.regions.Region;
import com.nike.cerberus.aws.KmsClientFactory;

public class HystrixKmsClientFactory extends KmsClientFactory {

    private final KmsClientFactory kmsClientFactory;

    public HystrixKmsClientFactory(KmsClientFactory kmsClientFactory) {
        this.kmsClientFactory = kmsClientFactory;
    }

    @Override
    public HystrixKmsClient getClient(Region region) {
        return new HystrixKmsClient(kmsClientFactory.getClient(region));
    }

    @Override
    public HystrixKmsClient getClient(String regionName) {
        return new HystrixKmsClient(kmsClientFactory.getClient(regionName));
    }
}