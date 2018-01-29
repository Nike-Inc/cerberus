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

package com.nike.cerberus.endpoints.secret;

import com.nike.cerberus.domain.VaultStyleErrorResponse;
import com.nike.cerberus.service.PermissionsService;
import com.nike.cerberus.service.SafeDepositBoxService;
import com.nike.cerberus.service.SecureDataService;
import com.nike.riposte.server.http.RequestInfo;
import com.nike.riposte.server.http.ResponseInfo;
import com.nike.riposte.util.Matcher;
import io.netty.channel.ChannelHandlerContext;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static junit.framework.TestCase.assertEquals;
import static org.mockito.MockitoAnnotations.initMocks;

public class SecureDataEndpointV1Test {

    @Mock
    private SecureDataService secureDataService;
    @Mock
    private PermissionsService permissionService;
    @Mock
    private SafeDepositBoxService safeDepositBoxService;

    private SUT sut;

    @Before public void before() {
        initMocks(this);
        sut = new SUT(secureDataService, permissionService, safeDepositBoxService);
    }

    @Test
    public void test_that_extra_slashes_get_trimmed() {
        String path = "/v1/secret//shared/okta//api_key";
        SecureDataEndpointV1.SecureDataRequestInfo info = sut.parseInfoFromPath(path);
        assertEquals("okta/api_key", info.getPath());
        assertEquals("shared", info.getCategory());
    }

    @Test
    public void test_that_happy_path_path_works() {
        String path = "/v1/secret/shared/okta/api_key";
        SecureDataEndpointV1.SecureDataRequestInfo info = sut.parseInfoFromPath(path);
        assertEquals("okta/api_key", info.getPath());
        assertEquals("shared", info.getCategory());
    }

    class SUT extends SecureDataEndpointV1<Void, Void> {

        protected SUT(SecureDataService secureDataService, PermissionsService permissionService, SafeDepositBoxService safeDepositBoxService) {
            super(secureDataService, permissionService, safeDepositBoxService);
        }

        @Override
        protected ResponseInfo<Void> generateVaultStyleResponse(VaultStyleErrorResponse response, int statusCode) {
            return null;
        }

        @Override
        protected CompletableFuture<ResponseInfo<Void>> doExecute(SecureDataRequestInfo requestInfo, RequestInfo<Void> request, Executor longRunningTaskExecutor, ChannelHandlerContext ctx) {
            return null;
        }

        @Override
        public Matcher requestMatcher() {
            return null;
        }
    }

}
