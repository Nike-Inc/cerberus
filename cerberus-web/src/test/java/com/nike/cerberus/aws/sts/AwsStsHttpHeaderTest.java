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

package com.nike.cerberus.aws.sts;

import static junit.framework.TestCase.assertEquals;

import org.junit.Test;

public class AwsStsHttpHeaderTest {

  @Test
  public void test_getRegion_returns_region_as_expected() {
    AwsStsHttpHeader header =
        new AwsStsHttpHeader(
            "20180904T205115Z",
            "FQoGZXIvYXdzEFYaDEYceadsfLKJLKlkj908098oB/rJIdxdo57fx3Ef2wW8WhFbSpLGg3hwNqhuepdkf/c0F7OXJutqM2yjgnZCiO7SPAdnMSJhoEgH7SJlkPaPfiRzZAf0yxxD6e4z0VJU74uQfbgfZpn5RL+JyDpgoYkUrjuyL8zRB1knGSOCi32Q75+asdfasd+7bWxMyJIKEb/HF2Le8xM/9F4WRqa5P0+asdfasdfasdf+MGlDlNG0KTzg1JT6QXf95ozWR5bBFSz5DbrFhXhMegMQ7+7Kvx+asdfasdl.jlkj++5NpRRlE54cct7+aG3HQskow9y73AU=",
            "AWS4-HMAC-SHA256 Credential=ASIA5S2FQS2GYQLK5FFF/20180904/us-west-2/sts/aws4_request, SignedHeaders=host;x-amz-date, Signature=ddb9417d2b9bfe6f8b03e31a8f5d8ab98e0f4alkj12312098asdf");

    assertEquals("us-west-2", header.getRegion());

    header =
        new AwsStsHttpHeader(
            "20180904T205115Z",
            "FQoGZXIvYXdzEFYaDEYceadsfLKJLKlkj908098oB/rJIdxdo57fx3Ef2wW8WhFbSpLGg3hwNqhuepdkf/c0F7OXJutqM2yjgnZCiO7SPAdnMSJhoEgH7SJlkPaPfiRzZAf0yxxD6e4z0VJU74uQfbgfZpn5RL+JyDpgoYkUrjuyL8zRB1knGSOCi32Q75+asdfasd+7bWxMyJIKEb/HF2Le8xM/9F4WRqa5P0+asdfasdfasdf+MGlDlNG0KTzg1JT6QXf95ozWR5bBFSz5DbrFhXhMegMQ7+7Kvx+asdfasdl.jlkj++5NpRRlE54cct7+aG3HQskow9y73AU=",
            "AWS4-HMAC-SHA256 Credential=ASIA5S2FQS2GYQLK5FFF/20180904/us-east-1/sts/aws4_request, SignedHeaders=host;x-amz-date, Signature=ddb9417d2b9bfe6f8b03e31a8f5d8ab98e0f4alkj12312098asdf");

    assertEquals("us-east-1", header.getRegion());
  }
}
