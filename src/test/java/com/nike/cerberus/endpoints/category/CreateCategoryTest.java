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

import com.nike.backstopper.exception.ApiException;
import com.nike.cerberus.domain.Category;
import com.nike.cerberus.service.CategoryService;
import com.nike.riposte.server.http.RequestInfo;
import com.nike.riposte.server.http.ResponseInfo;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.SecurityContext;
import java.security.Principal;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static com.nike.cerberus.endpoints.category.CreateCategory.CATEGORY_PATH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CreateCategoryTest {

    private final String user = "USER";

    private final String categoryId = "CATEGORY_ID";

    private final Category category = new Category().setId(categoryId);

    private final Executor executor = Executors.newSingleThreadExecutor();

    private CategoryService categoryService;

    private CreateCategory subject;

    @Before
    public void setUp() throws Exception {
        categoryService = mock(CategoryService.class);
        subject = new CreateCategory(categoryService);
    }

    @Test
    public void requestMatcher_is_http_post() {
        final Collection<HttpMethod> httpMethods = subject.requestMatcher().matchingMethods();

        assertThat(httpMethods).hasSize(1);
        assertThat(httpMethods).contains(HttpMethod.POST);
    }

    @Test
    public void doExecute_returns_201_with_location() {
        final RequestInfo<Category> requestInfo = mock(RequestInfo.class);
        when(requestInfo.getContent()).thenReturn(category);
        final Principal principal = mock(Principal.class);
        when(principal.getName()).thenReturn(user);
        final SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getUserPrincipal()).thenReturn(principal);
        when(categoryService.createCategory(category, user)).thenReturn(categoryId);

        final CompletableFuture<ResponseInfo<Void>> completableFuture =
                subject.doExecute(requestInfo, executor, null, securityContext);
        final ResponseInfo<Void> responseInfo = completableFuture.join();
        assertThat(responseInfo.getHttpStatusCode()).isEqualTo(HttpResponseStatus.CREATED.code());
        assertThat(responseInfo.getHeaders().get(HttpHeaders.Names.LOCATION)).isEqualTo(CATEGORY_PATH + "/" + categoryId);
    }

    @Test
    public void doExecute_throws_error_when_request_body_is_null() {
        final RequestInfo<Category> requestInfo = mock(RequestInfo.class);
        when(requestInfo.getContent()).thenReturn(null);

        try {
            final CompletableFuture<ResponseInfo<Void>> completableFuture =
                    subject.doExecute(requestInfo, executor, null, null);
            completableFuture.join();
            fail("Expected exception not thrown.");
        } catch (CompletionException cex) {
            assertThat(cex.getCause().getClass()).isEqualTo(ApiException.class);
        }
    }
}