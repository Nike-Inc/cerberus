/*
 * Copyright (c) 2016 Nike, Inc.
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

package com.nike.cerberus.endpoints;

import com.google.common.collect.Maps;
import com.nike.backstopper.exception.ApiException;
import com.nike.cerberus.domain.CerberusAuthToken;
import com.nike.cerberus.security.CmsRequestSecurityValidator;
import com.nike.cerberus.security.CerberusPrincipal;
import com.nike.cerberus.security.CerberusSecurityContext;
import com.nike.cerberus.service.EventProcessorService;
import com.nike.riposte.server.http.RequestInfo;
import com.nike.riposte.server.http.ResponseInfo;
import com.nike.riposte.util.Matcher;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import javax.ws.rs.core.SecurityContext;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class AdminStandardEndpointTest {

    private final Executor executor = Executors.newSingleThreadExecutor();

    @Mock
    private EventProcessorService eventProcessorService;

    private AdminStandardEndpoint<Void, Void> subject;

    @Before
    public void before() throws Exception {
        initMocks(this);

        subject = new AdminStandardEndpointImpl();
        subject.setEventProcessorService(eventProcessorService);
    }

    @Test
    public void execute_validates_user_is_admin() {
        final Map<String, Object> requestAttributes = Maps.newHashMap();
        final CerberusPrincipal authPrincipal = new CerberusPrincipal(
                CerberusAuthToken.Builder.create()
                        .withIsAdmin(true)
                        .withPrincipal("username")
                        .withGroups("group1,group2")
                        .build());
        requestAttributes.put(CmsRequestSecurityValidator.SECURITY_CONTEXT_ATTR_KEY,
                new CerberusSecurityContext(authPrincipal, "https"));
        final RequestInfo<Void> requestInfo = mock(RequestInfo.class);
        when(requestInfo.getRequestAttributes()).thenReturn(requestAttributes);

        final CompletableFuture<ResponseInfo<Void>> completableFuture = subject.execute(requestInfo, executor, null);
        final ResponseInfo<Void> responseInfo = completableFuture.join();

        assertThat(responseInfo.getHttpStatusCode()).isEqualTo(HttpResponseStatus.OK.code());
    }

    @Test(expected = ApiException.class)
    public void execute_throws_error_if_security_context_not_present() {
        final Map<String, Object> requestAttributes = Maps.newHashMap();
        final RequestInfo<Void> requestInfo = mock(RequestInfo.class);
        when(requestInfo.getRequestAttributes()).thenReturn(requestAttributes);

        subject.execute(requestInfo, executor, null);
    }

    @Test(expected = ApiException.class)
    public void execute_throws_error_if_principal_not_an_admin() {
        final Map<String, Object> requestAttributes = Maps.newHashMap();
        final CerberusPrincipal authPrincipal = new CerberusPrincipal(
                CerberusAuthToken.Builder.create()
                        .withIsAdmin(false)
                        .withPrincipal("username")
                        .withGroups("group1,group2")
                        .build());
        requestAttributes.put(CmsRequestSecurityValidator.SECURITY_CONTEXT_ATTR_KEY,
                new CerberusSecurityContext(authPrincipal, "https"));
        final RequestInfo<Void> requestInfo = mock(RequestInfo.class);
        when(requestInfo.getRequestAttributes()).thenReturn(requestAttributes);

        subject.execute(requestInfo, executor, null);
    }

    public class AdminStandardEndpointImpl extends AdminStandardEndpoint<Void, Void> {

        @Override
        public CompletableFuture<ResponseInfo<Void>> doExecute(RequestInfo<Void> request,
                                                               Executor longRunningTaskExecutor,
                                                               ChannelHandlerContext ctx,
                                                               SecurityContext securityContext) {
            return CompletableFuture.completedFuture(
                    ResponseInfo.<Void>newBuilder().withHttpStatusCode(HttpResponseStatus.OK.code()).build());
        }

        @Override
        public Matcher requestMatcher() {
            return Matcher.match("");
        }
    }
}