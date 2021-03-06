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

package com.nike.cerberus.audit.logger;

import com.amazonaws.services.athena.AmazonAthena;
import com.amazonaws.services.athena.AmazonAthenaClient;
import com.google.common.collect.Maps;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class AthenaClientFactory {

  private final Map<String, AmazonAthena> athenaClientMap = Maps.newConcurrentMap();

  public AmazonAthena getClient(String region) {
    AmazonAthena client = athenaClientMap.get(region);

    if (client == null) {
      client = AmazonAthenaClient.builder().withRegion(region).build();
      athenaClientMap.put(region, client);
    }

    return client;
  }
}
