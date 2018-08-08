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

import com.nike.riposte.server.http.RequestInfo;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpHeaders;
import org.junit.Assert;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AwsStsHttpHeadersTest {

    @Test
    public void test_get_date() {
        String date = "Tue, 07 Aug 2018 22:28:20 UTC";
        HttpHeaders headers = new DefaultHttpHeaders();
        headers.add(AwsStsHttpHeaders.HEADER_DATE, date);
        RequestInfo request = mock(RequestInfo.class);
        when(request.getHeaders()).thenReturn(headers);

        Assert.assertEquals(date, AwsStsHttpHeaders.getDate(request));
    }

    @Test
    public void test_get_date_when_null() {
        RequestInfo request = mock(RequestInfo.class);

        Assert.assertEquals(null, AwsStsHttpHeaders.getDate(request));
    }

    @Test
    public void test_get_x_amz_date() {
        String date = "20180807T222820Z";
        HttpHeaders headers = new DefaultHttpHeaders();
        headers.add(AwsStsHttpHeaders.HEADER_X_AMZ_DATE, date);
        RequestInfo request = mock(RequestInfo.class);
        when(request.getHeaders()).thenReturn(headers);

        Assert.assertEquals(date, AwsStsHttpHeaders.getHeaderXAmzDate(request));
    }

    @Test
    public void test_get_x_amz_date_when_null() {
        RequestInfo request = mock(RequestInfo.class);

        Assert.assertEquals(null, AwsStsHttpHeaders.getHeaderXAmzDate(request));
    }

    @Test
    public void test_get_x_amz_security_token() {
        String token = "test token";
        HttpHeaders headers = new DefaultHttpHeaders();
        headers.add(AwsStsHttpHeaders.HEADER_X_AMZ_SECURITY_TOKEN, token);
        RequestInfo request = mock(RequestInfo.class);
        when(request.getHeaders()).thenReturn(headers);

        Assert.assertEquals(token, AwsStsHttpHeaders.getHeaderXAmzSecurityToken(request));
    }

    @Test
    public void test_get_x_amz_security_when_null() {
        RequestInfo request = mock(RequestInfo.class);

        Assert.assertEquals(null, AwsStsHttpHeaders.getHeaderXAmzSecurityToken(request));
    }

    @Test
    public void test_get_authorization() {
        String authorization = "test authorization";
        HttpHeaders headers = new DefaultHttpHeaders();
        headers.add(AwsStsHttpHeaders.HEADER_AUTHORIZATION, authorization);
        RequestInfo request = mock(RequestInfo.class);
        when(request.getHeaders()).thenReturn(headers);

        Assert.assertEquals(authorization, AwsStsHttpHeaders.getHeaderAuthorization(request));
    }

    @Test
    public void test_get_authorization_when_null() {
        RequestInfo request = mock(RequestInfo.class);

        Assert.assertEquals(null, AwsStsHttpHeaders.getHeaderAuthorization(request));
    }
}