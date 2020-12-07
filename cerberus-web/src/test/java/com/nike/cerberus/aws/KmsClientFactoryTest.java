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

package com.nike.cerberus.aws;

import static org.assertj.core.api.Assertions.assertThat;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.kms.AWSKMSClient;
import com.nike.backstopper.exception.ApiException;
import org.junit.Before;
import org.junit.Test;

/** Tests the KmsClientFactory class */
public class KmsClientFactoryTest {

  private static final String goodRegionName = "us-west-2";

  private final Region goodRegion = Region.getRegion(Regions.fromName(goodRegionName));

  private static final String badRegionName = "zz-space-1";

  private KmsClientFactory subject;

  @Before
  public void setup() {
    subject = new KmsClientFactory();
  }

  @Test
  public void get_client_by_region_returns_configured_kms_client() {
    AWSKMSClient client = subject.getClient(goodRegion);

    assertThat(client).isNotNull();
  }

  @Test
  public void get_client_by_region_string_returns_configured_kms_client() {
    AWSKMSClient client = subject.getClient(goodRegionName);

    assertThat(client).isNotNull();
  }

  @Test(expected = ApiException.class)
  public void get_client_by_region_string_throws_exception_if_bad_region_passed() {
    subject.getClient(badRegionName);
  }
}
