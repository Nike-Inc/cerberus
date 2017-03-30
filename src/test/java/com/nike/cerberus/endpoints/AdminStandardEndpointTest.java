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
import com.nike.cerberus.security.CmsRequestSecurityValidator;
import com.nike.cerberus.security.VaultAuthPrincipalV1;
import com.nike.cerberus.security.VaultSecurityContext;
import com.nike.vault.client.model.VaultClientTokenResponse;
import com.nike.riposte.server.http.RequestInfo;
import com.nike.riposte.server.http.ResponseInfo;
import com.nike.riposte.util.Matcher;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.SecurityContext;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AdminStandardEndpointTest {

    private final Executor executor = Executors.newSingleThreadExecutor();

    private AdminStandardEndpoint<Void, Void> subject;

    @Before
    public void setUp() throws Exception {
        subject = new AdminStandardEndpointImpl();
    }

    @Test
    public void execute_validates_user_is_admin() {
        final Map<String, Object> requestAttributes = Maps.newHashMap();
        final Map<String, String> meta = Maps.newHashMap();
        meta.put(VaultAuthPrincipalV1.METADATA_KEY_IS_ADMIN, Boolean.TRUE.toString());
        meta.put(VaultAuthPrincipalV1.METADATA_KEY_USERNAME, "username");
        meta.put(VaultAuthPrincipalV1.METADATA_KEY_GROUPS, "group1,group2");
        final VaultAuthPrincipalV1 authPrincipal = new VaultAuthPrincipalV1(new VaultClientTokenResponse().setMeta(meta));
        requestAttributes.put(CmsRequestSecurityValidator.SECURITY_CONTEXT_ATTR_KEY,
                new VaultSecurityContext(authPrincipal, "https"));
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
        final Map<String, String> meta = Maps.newHashMap();
        meta.put(VaultAuthPrincipalV1.METADATA_KEY_IS_ADMIN, Boolean.FALSE.toString());
        meta.put(VaultAuthPrincipalV1.METADATA_KEY_USERNAME, "username");
        meta.put(VaultAuthPrincipalV1.METADATA_KEY_GROUPS, "group1,group2");
        final VaultAuthPrincipalV1 authPrincipal = new VaultAuthPrincipalV1(new VaultClientTokenResponse().setMeta(meta));
        requestAttributes.put(CmsRequestSecurityValidator.SECURITY_CONTEXT_ATTR_KEY,
                new VaultSecurityContext(authPrincipal, "https"));
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