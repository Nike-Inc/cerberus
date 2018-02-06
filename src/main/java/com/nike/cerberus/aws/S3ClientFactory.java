/*
 * Copyright (c) 2018 Nike, Inc.
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

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.google.common.collect.Maps;

import java.util.Map;

public class S3ClientFactory {

    private final Map<String, AmazonS3> s3ClientMap = Maps.newConcurrentMap();

    public AmazonS3 getClient(String region) {
        AmazonS3 client = s3ClientMap.get(region);

        if (client == null) {
            client = AmazonS3Client.builder().withRegion(region).build();
            s3ClientMap.put(region, client);
        }

        return client;
    }
}
