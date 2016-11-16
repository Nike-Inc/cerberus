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

import com.google.common.collect.Lists;
import com.nike.cerberus.domain.Category;
import com.nike.cerberus.service.CategoryService;
import com.nike.riposte.server.http.ResponseInfo;
import io.netty.handler.codec.http.HttpMethod;
import org.junit.Before;
import org.junit.Test;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GetAllCategoriesTest {

    private final String categoryId = "CATEGORY_ID";

    private final Category category = new Category().setId(categoryId);

    private final List<Category> categoryList = Lists.newArrayList(category);

    private final Executor executor = Executors.newSingleThreadExecutor();

    private CategoryService categoryService;

    private GetAllCategories subject;

    @Before
    public void setUp() throws Exception {
        categoryService = mock(CategoryService.class);
        subject = new GetAllCategories(categoryService);
    }

    @Test
    public void requestMatcher_is_http_get() {
        final Collection<HttpMethod> httpMethods = subject.requestMatcher().matchingMethods();

        assertThat(httpMethods).hasSize(1);
        assertThat(httpMethods).contains(HttpMethod.GET);
    }

    @Test
    public void execute_returns_list_of_categories() {
        when(categoryService.getAllCategories()).thenReturn(categoryList);

        final CompletableFuture<ResponseInfo<List<Category>>> completableFuture =
                subject.execute(null, executor, null);
        final ResponseInfo<List<Category>> responseInfo = completableFuture.join();

        assertThat(responseInfo.getContentForFullResponse()).containsOnly(category);
    }
}