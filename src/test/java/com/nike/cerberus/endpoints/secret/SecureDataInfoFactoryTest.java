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

import com.nike.cerberus.SecureDataRequestService;
import com.nike.cerberus.domain.CerberusAuthToken;
import com.nike.cerberus.domain.SecureDataRequestInfo;
import com.nike.cerberus.security.CerberusPrincipal;
import com.nike.cerberus.security.CerberusSecurityContext;
import com.nike.cerberus.service.PermissionsService;
import com.nike.cerberus.service.SafeDepositBoxService;
import com.nike.cerberus.service.SecureDataService;
import com.nike.riposte.server.http.RequestInfo;
import io.netty.handler.codec.http.HttpMethod;
import org.assertj.core.util.Maps;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Map;
import java.util.Optional;

import static junit.framework.TestCase.assertEquals;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class SecureDataInfoFactoryTest {

    @Mock
    private SecureDataService secureDataService;
    @Mock
    private PermissionsService permissionService;
    @Mock
    private SafeDepositBoxService safeDepositBoxService;

    private SecureDataRequestService secureDataRequestService;

    @Before public void before() {
        initMocks(this);
        secureDataRequestService = new SecureDataRequestService(safeDepositBoxService, permissionService);
    }

    @Test
    public void test_that_extra_slashes_get_trimmed() {
        String sdbId = "sdb id";
        String sdbPath = "shared/okta/";
        String path = "/v1/secret//shared/okta//api_key";
        RequestInfo requestInfo = mock(RequestInfo.class);
        Map<String, Object> attributes = Maps.newHashMap();

        attributes.put("cerberusSecurityContext", new CerberusSecurityContext(new CerberusPrincipal(new CerberusAuthToken()), null));
        when(requestInfo.getMethod()).thenReturn(HttpMethod.GET);
        when(requestInfo.getRequestAttributes()).thenReturn(attributes);
        when(requestInfo.getPath()).thenReturn(path);
        when(safeDepositBoxService.getSafeDepositBoxIdByPath(sdbPath)).thenReturn(Optional.of(sdbId));
        when(permissionService.doesPrincipalHavePermission(anyObject(), anyObject(), anyObject())).thenReturn(true);

        SecureDataRequestInfo info = secureDataRequestService.parseAndValidateRequest(requestInfo);
        assertEquals("okta/api_key", info.getPath());
        assertEquals("shared", info.getCategory());
    }

    @Test
    public void test_that_happy_path_path_works() {
        String sdbId = "sdb id";
        String sdbPath = "shared/okta/";
        String path = "/v1/secret/shared/okta/api_key";
        RequestInfo requestInfo = mock(RequestInfo.class);
        Map<String, Object> attributes = Maps.newHashMap();

        attributes.put("cerberusSecurityContext", new CerberusSecurityContext(new CerberusPrincipal(new CerberusAuthToken()), null));
        when(requestInfo.getMethod()).thenReturn(HttpMethod.GET);
        when(requestInfo.getRequestAttributes()).thenReturn(attributes);
        when(requestInfo.getPath()).thenReturn(path);
        when(safeDepositBoxService.getSafeDepositBoxIdByPath(sdbPath)).thenReturn(Optional.of(sdbId));
        when(permissionService.doesPrincipalHavePermission(anyObject(), anyObject(), anyObject())).thenReturn(true);

        SecureDataRequestInfo info = secureDataRequestService.parseAndValidateRequest(requestInfo);
        assertEquals("okta/api_key", info.getPath());
        assertEquals("shared", info.getCategory());
    }

}
