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

package com.nike.cerberus.service;

import com.nike.cerberus.aws.S3ClientFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static junit.framework.TestCase.assertEquals;
import static org.mockito.MockitoAnnotations.initMocks;

public class S3LogUploaderServiceTest {

    @Mock
    private ConfigService configService;

    @Mock
    AthenaService athenaService;

    @Mock
    S3ClientFactory s3ClientFactory;

    private S3LogUploaderService s3LogUploader;

    @Before
    public void before() {
        initMocks(this);
        s3LogUploader = new S3LogUploaderService("fake-bucket", "us-west-2", configService, athenaService, s3ClientFactory);
    }

    @Test
    public void test_that_getPartition_works() {
        String fileName = "localhost-audit.2018-01-29_12-58.log.gz";
        String actual = s3LogUploader.getPartition(fileName);
        String expected = "partitioned/year=2018/month=01/day=29/hour=12";
        assertEquals(expected, actual);
    }

}
