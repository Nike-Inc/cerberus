/*
 * Copyright (c) 2017 Nike, Inc.
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

package com.nike.cerberus.endpoints.admin;

import com.nike.backstopper.exception.ApiException;
import com.nike.cerberus.service.MetaDataService;
import com.nike.riposte.server.http.RequestInfo;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class GetSDBMetaDataTest {

    @InjectMocks
    private GetSDBMetaData endpoint;

    private GetSDBMetaData endpointSpy;

    @Mock
    MetaDataService metaDataService;

    @Mock
    RequestInfo<Void> request;

    @Before
    public void before() {
        initMocks(this);
        endpointSpy = spy(endpoint);
    }

    @Test
    public void test_that_get_limit_returns_default_value_when_no_limit_is_supplied() {
        assertEquals(GetSDBMetaData.DEFAULT_LIMIT, endpointSpy.getLimit(request));
    }

    @Test
    public void test_that_get_offset_returns_default_value_when_no_limit_is_supplied() {
        assertEquals(GetSDBMetaData.DEFAULT_OFFSET, endpointSpy.getOffset(request));
    }

    @Test
    public void test_that_get_limit_returns_supplied_value_when_limit_is_supplied() {
        when(request.getQueryParamSingle(GetSDBMetaData.LIMIT_QUERY_KEY)).thenReturn("7");
        assertEquals(7, endpointSpy.getLimit(request));
    }

    @Test
    public void test_that_get_offset_returns_supplied_value_when_limit_is_supplied() {
        when(request.getQueryParamSingle(GetSDBMetaData.OFFSET_QUERY_KEY)).thenReturn("6");
        assertEquals(6, endpointSpy.getOffset(request));
    }

    @Test(expected = ApiException.class)
    public void test_that_a_bad_request_is_thrown_if_limit_is_less_than_1() {
        endpoint.validateLimitQuery("0");
    }

    @Test(expected = ApiException.class)
    public void test_that_a_bad_request_is_thrown_if_offset_is_less_than_0() {
        endpoint.validateOffsetQuery("-1");
    }

    @Test(expected = ApiException.class)
    public void test_that_a_bad_request_is_thrown_if_limit_is_non_numeric() {
        endpoint.validateLimitQuery("abc");
    }

    @Test(expected = ApiException.class)
    public void test_that_a_bad_request_is_thrown_if_offset_is_non_numeric() {
        endpoint.validateOffsetQuery("abc");
    }
}
