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
import com.nike.cerberus.endpoints.AdminStandardEndpoint;
import com.nike.cerberus.error.DefaultApiError;
import com.nike.cerberus.security.CerberusPrincipal;
import com.nike.riposte.server.http.RequestInfo;
import com.nike.riposte.server.http.ResponseInfo;
import com.nike.riposte.util.Matcher;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.apache.commons.lang3.StringUtils;
import org.knowm.sundial.SundialJobScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.SecurityContext;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class TriggerScheduledJob extends AdminStandardEndpoint<Void, Void> {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public CompletableFuture<ResponseInfo<Void>> doExecute(RequestInfo<Void> request,
                                                           Executor longRunningTaskExecutor,
                                                           ChannelHandlerContext ctx,
                                                           SecurityContext securityContext) {

        String job = request.getPathParam("job");

        List<String> registeredJobs = SundialJobScheduler.getAllJobNames();

        if (StringUtils.isBlank(job) || ! registeredJobs.contains(job)) {
            log.error("The Job: {} was null or was not registered. Registered jobs are {}",
                    job, String.join(",", registeredJobs));
            throw new ApiException(DefaultApiError.GENERIC_BAD_REQUEST);
        }

        SundialJobScheduler.startJob(job);

        return CompletableFuture.completedFuture(
                ResponseInfo.<Void>newBuilder()
                        .withHttpStatusCode(HttpResponseStatus.NO_CONTENT.code())
                        .build()
        );
    }

    @Override
    public Matcher requestMatcher() {
        return Matcher.match("/v1/admin/trigger-job/{job}", HttpMethod.POST);
    }

    @Override
    protected String describeActionForAuditEvent(RequestInfo<Void> request) {
        return String.format("Triggering job: %s.", request.getPathParam("job"));
    }
}
