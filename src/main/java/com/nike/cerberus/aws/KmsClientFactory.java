/*
 * Copyright (c) 2016 Nike, Inc.
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
 */

package com.nike.cerberus.aws;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.kms.AWSKMSClient;
import com.google.common.collect.Maps;
import com.nike.backstopper.exception.ApiException;
import com.nike.cerberus.error.DefaultApiError;

import javax.inject.Singleton;
import java.util.Map;

/**
 * Factory for AWS KMS clients.  Caches clients by region as they are requested.
 */
public class KmsClientFactory {

    private final Map<Region, AWSKMSClient> kmsClientMap = Maps.newConcurrentMap();

    /**
     * Returns a KMS client for the given region.  Clients are cached by region.
     *
     * @param region Region to configure a client for
     * @return AWS KMS client
     */
    public AWSKMSClient getClient(Region region) {
        AWSKMSClient client = kmsClientMap.get(region);

        if (client == null) {
            final AWSKMSClient newClient = new AWSKMSClient();
            newClient.setRegion(region);
            kmsClientMap.put(region, newClient);
            client = newClient;
        }

        return client;
    }

    /**
     * Returns a KMS client for the given region name.  Clients are cached by region.
     *
     * @param regionName Region to configure a client for
     * @return AWS KMS client
     */
    public AWSKMSClient getClient(String regionName) {
        try {
            final Region region = Region.getRegion(Regions.fromName(regionName));
            return getClient(region);
        } catch (IllegalArgumentException iae) {
            throw ApiException.newBuilder()
                    .withApiErrors(DefaultApiError.AUTHENTICATION_ERROR_INVALID_REGION)
                    .withExceptionCause(iae.getCause())
                    .withExceptionMessage("Specified region is not valid.")
                    .build();
        }
    }
}
