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

package com.nike.cerberus.endpoints.stats;

import com.google.common.collect.Sets;
import com.nike.cerberus.domain.Stats;
import com.nike.cerberus.service.StatsService;
import com.nike.riposte.server.http.ResponseInfo;
import io.netty.handler.codec.http.HttpMethod;
import org.junit.Before;
import org.junit.Test;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GetStatsTest {

    private final OffsetDateTime now = OffsetDateTime.now(ZoneId.of("UTC"));

    private final Stats stats = new Stats().setSafeDepositBoxStats(Sets.newHashSet()).setGeneratedTs(now);

    private Executor executor = Executors.newSingleThreadExecutor();

    private StatsService statsService;

    private GetStats subject;

    @Before
    public void setUp() throws Exception {
        statsService = mock(StatsService.class);
        subject = new GetStats(statsService);
    }

    @Test
    public void requestMatcher_is_http_get() {
        final Collection<HttpMethod> httpMethods = subject.requestMatcher().matchingMethods();

        assertThat(httpMethods).containsOnly(HttpMethod.GET);
    }

    @Test
    public void doExecute_returns_stats() {
        when(statsService.getStats()).thenReturn(stats);

        final CompletableFuture<ResponseInfo<Stats>> completableFuture =
                subject.doExecute(null, executor, null, null);
        final ResponseInfo<Stats> responseInfo = completableFuture.join();

        assertThat(responseInfo.getContentForFullResponse()).isEqualToComparingFieldByField(stats);
    }
}