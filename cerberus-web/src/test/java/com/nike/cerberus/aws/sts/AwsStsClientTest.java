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

package com.nike.cerberus.aws.sts;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;

public class AwsStsClientTest {

  private AwsStsHttpClient httpClient;
  private AwsStsClient awsStsClient;
  private AwsStsHttpHeader awsStsHttpHeader;

  @Before
  public void setup() {
    httpClient = mock(AwsStsHttpClient.class);
    awsStsClient = new AwsStsClient(httpClient);
    awsStsHttpHeader =
        new AwsStsHttpHeader(
            "test amz date",
            "test amz security token",
            "AWS4-HMAC-SHA256 Credential=ASIA5S2FQS2GYQLK5FFF/20180904/us-west-2/sts/aws4_request, SignedHeaders=host;x-amz-date, Signature=ddb9417d2b9bfe6f8b03e31a8f5d8ab98e0f4alkj12312098asdf");
  }

  @Test
  public void test_getCallerIdentity() {

    setupMocks();

    GetCallerIdentityFullResponse response = mock(GetCallerIdentityFullResponse.class);

    when(httpClient.execute(
            awsStsHttpHeader.getRegion(),
            awsStsHttpHeader.generateHeaders(),
            GetCallerIdentityFullResponse.class))
        .thenReturn(response);

    // invoke method under test
    GetCallerIdentityResponse actualResponse = awsStsClient.getCallerIdentity(awsStsHttpHeader);

    assertEquals(response.getGetCallerIdentityResponse(), actualResponse);
  }

  private void setupMocks() {

    GetCallerIdentityFullResponse response = new GetCallerIdentityFullResponse();

    when(httpClient.execute(
            awsStsHttpHeader.getRegion(),
            awsStsHttpHeader.generateHeaders(),
            GetCallerIdentityFullResponse.class))
        .thenReturn(response);
  }
}
