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
 *
 */

package com.nike.cerberus.service;

import com.nike.backstopper.exception.ApiException;
import com.nike.riposte.server.http.RequestInfo;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class PaginationServiceTest {

    @InjectMocks
    private PaginationService paginationService;

    private PaginationService paginationServiceSpy;

    @Mock
    RequestInfo<Void> request;

    @Before
    public void before() {
        initMocks(this);
        paginationServiceSpy = spy(paginationService);
    }

    @Test
    public void test_that_get_limit_returns_default_value_when_no_limit_is_supplied() {
        assertEquals(PaginationService.DEFAULT_LIMIT, paginationServiceSpy.getLimit(request));
    }

    @Test
    public void test_that_get_offset_returns_default_value_when_no_limit_is_supplied() {
        Assert.assertEquals(PaginationService.DEFAULT_OFFSET, paginationServiceSpy.getOffset(request));
    }

    @Test
    public void test_that_get_limit_returns_supplied_value_when_limit_is_supplied() {
        when(request.getQueryParamSingle(PaginationService.LIMIT_QUERY_KEY)).thenReturn("7");
        assertEquals(7, paginationServiceSpy.getLimit(request));
    }

    @Test
    public void test_that_get_offset_returns_supplied_value_when_limit_is_supplied() {
        when(request.getQueryParamSingle(PaginationService.OFFSET_QUERY_KEY)).thenReturn("6");
        assertEquals(6, paginationServiceSpy.getOffset(request));
    }

    @Test(expected = ApiException.class)
    public void test_that_a_bad_request_is_thrown_if_limit_is_less_than_1() {
        paginationServiceSpy.validateLimitQuery("0");
    }

    @Test(expected = ApiException.class)
    public void test_that_a_bad_request_is_thrown_if_offset_is_less_than_0() {
        paginationServiceSpy.validateOffsetQuery("-1");
    }

    @Test(expected = ApiException.class)
    public void test_that_a_bad_request_is_thrown_if_limit_is_non_numeric() {
        paginationServiceSpy.validateLimitQuery("abc");
    }

    @Test(expected = ApiException.class)
    public void test_that_a_bad_request_is_thrown_if_offset_is_non_numeric() {
        paginationServiceSpy.validateOffsetQuery("abc");
    }
}
