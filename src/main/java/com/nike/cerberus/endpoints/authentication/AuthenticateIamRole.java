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

package com.nike.cerberus.endpoints.authentication;

import com.nike.backstopper.apierror.ApiError;
import com.nike.backstopper.exception.ApiException;
import com.nike.cerberus.domain.IamRoleAuthResponse;
import com.nike.cerberus.domain.IamRoleCredentials;
import com.nike.cerberus.service.AuthenticationService;
import com.nike.cerberus.service.EventProcessorService;
import com.nike.cerberus.util.AwsIamRoleArnParser;
import com.nike.riposte.server.http.RequestInfo;
import com.nike.riposte.server.http.ResponseInfo;
import com.nike.riposte.server.http.StandardEndpoint;
import com.nike.riposte.util.AsyncNettyHelper;
import com.nike.riposte.util.Matcher;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

import static com.nike.cerberus.endpoints.AuditableEventEndpoint.auditableEvent;

/**
 * Authentication endpoint for IAM roles.  If valid, a client token that is encrypted via KMS is returned.  The
 * IAM role will be the only role capable of decrypting the client token via KMS.
 */
@Deprecated
public class AuthenticateIamRole extends StandardEndpoint<IamRoleCredentials, IamRoleAuthResponse> {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final AuthenticationService authenticationService;
    private final EventProcessorService eventProcessorService;

    @Inject
    public AuthenticateIamRole(final AuthenticationService authenticationService,
                               EventProcessorService eventProcessorService) {

        this.authenticationService = authenticationService;
        this.eventProcessorService = eventProcessorService;
    }

    @Override
    public CompletableFuture<ResponseInfo<IamRoleAuthResponse>> execute(final RequestInfo<IamRoleCredentials> request,
                                                                        final Executor longRunningTaskExecutor,
                                                                        final ChannelHandlerContext ctx) {
        return CompletableFuture.supplyAsync(
                AsyncNettyHelper.supplierWithTracingAndMdc(() -> authenticate(request), ctx),
                longRunningTaskExecutor
        );

    }

    private ResponseInfo<IamRoleAuthResponse> authenticate(RequestInfo<IamRoleCredentials> request) {
        final IamRoleCredentials credentials = request.getContent();

        String arn = String.format(AwsIamRoleArnParser.AWS_IAM_ROLE_ARN_TEMPLATE,
                credentials.getAccountId(),
                credentials.getRoleName());

        IamRoleAuthResponse authResponse = null;
        try {
            authResponse = authenticationService.authenticate(request.getContent());
        } catch (ApiException e) {
            eventProcessorService.ingestEvent(auditableEvent(
                    arn, request, getClass().getSimpleName())
                    .withAction(String.format("Failed to authenticate in region %s, for reason: %s",
                            credentials.getRegion(),
                            String.join(",", e.getApiErrors().stream()
                                    .map(ApiError::getMessage).collect(Collectors.toList()))))
                    .withSuccess(false)
                    .build()
            );
            throw e;
        }

        eventProcessorService.ingestEvent(auditableEvent(
                arn, request, getClass().getSimpleName())
                .withAction(String.format("Successfully authenticated in region %s", credentials.getRegion()))
                .build()
        );

        return ResponseInfo.newBuilder(authResponse).build();
    }

    @Override
    public Matcher requestMatcher() {
        return Matcher.match("/v1/auth/iam-role", HttpMethod.POST);
    }
}
