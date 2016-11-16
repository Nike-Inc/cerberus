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

package com.nike.cerberus.endpoints.category;

import com.nike.cerberus.service.CategoryService;
import com.nike.riposte.server.http.RequestInfo;
import com.nike.riposte.server.http.ResponseInfo;
import com.nike.riposte.util.Matcher;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.SecurityContext;
import java.security.Principal;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static com.nike.cerberus.endpoints.category.DeleteCategory.PATH_PARAM_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DeleteCategoryTest {

    private final String user = "USER";

    private final String categoryId = "CATEGORY_ID";

    private Executor executor = Executors.newSingleThreadExecutor();

    private CategoryService categoryService;

    private DeleteCategory subject;

    @Before
    public void setUp() throws Exception {
        categoryService = mock(CategoryService.class);
        subject = new DeleteCategory(categoryService);
    }

    @Test
    public void requestMatcher_is_http_delete() {
        final Matcher matcher = subject.requestMatcher();
        assertThat(matcher.matchingMethods()).containsOnly(HttpMethod.DELETE);
    }

    @Test
    public void doExecute_returns_204_status_if_successful() {
        final RequestInfo<Void> requestInfo = mock(RequestInfo.class);
        when(requestInfo.getPathParam(PATH_PARAM_ID)).thenReturn(categoryId);
        final Principal principal = mock(Principal.class);
        when(principal.getName()).thenReturn(user);
        final SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getUserPrincipal()).thenReturn(principal);
        when(categoryService.deleteCategory(categoryId)).thenReturn(true);

        final CompletableFuture<ResponseInfo<Void>> completableFuture =
                subject.doExecute(requestInfo, executor, null, securityContext);
        final ResponseInfo<Void> responseInfo = completableFuture.join();

        assertThat(responseInfo.getHttpStatusCode()).isEqualTo(HttpResponseStatus.NO_CONTENT.code());
    }

    @Test
    public void doExecute_returns_404_status_if_not_found() {
        final RequestInfo<Void> requestInfo = mock(RequestInfo.class);
        when(requestInfo.getPathParam(PATH_PARAM_ID)).thenReturn(categoryId);
        final Principal principal = mock(Principal.class);
        when(principal.getName()).thenReturn(user);
        final SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getUserPrincipal()).thenReturn(principal);
        when(categoryService.deleteCategory(categoryId)).thenReturn(false);

        final CompletableFuture<ResponseInfo<Void>> completableFuture =
                subject.doExecute(requestInfo, executor, null, securityContext);
        final ResponseInfo<Void> responseInfo = completableFuture.join();

        assertThat(responseInfo.getHttpStatusCode()).isEqualTo(HttpResponseStatus.NOT_FOUND.code());
    }
}