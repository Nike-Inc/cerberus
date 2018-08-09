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
 *
 */

package com.nike.cerberus.endpoints.authentication;

import com.nike.backstopper.apierror.ApiError;
import com.nike.backstopper.exception.ApiException;
import com.nike.cerberus.aws.sts.AwsStsClient;
import com.nike.cerberus.aws.sts.AwsStsHttpHeader;
import com.nike.cerberus.aws.sts.GetCallerIdentityResponse;
import com.nike.cerberus.domain.AuthTokenResponse;
import com.nike.cerberus.error.DefaultApiError;
import com.nike.cerberus.service.AuthenticationService;
import com.nike.cerberus.service.EventProcessorService;
import com.nike.riposte.server.http.RequestInfo;
import com.nike.riposte.server.http.ResponseInfo;
import com.nike.riposte.server.http.StandardEndpoint;
import com.nike.riposte.util.AsyncNettyHelper;
import com.nike.riposte.util.Matcher;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpMethod;
import okhttp3.MediaType;

import javax.inject.Inject;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

import static com.nike.cerberus.aws.sts.AwsStsHttpHeaders.*;
import static com.nike.cerberus.endpoints.AuditableEventEndpoint.auditableEvent;

/**
 * Authentication endpoint for IAM roles.  If valid, a client token that is encrypted via KMS is returned.  The
 * IAM role will be the only role capable of decrypting the client token via KMS.
 */
public class AuthenticateStsIdentity extends StandardEndpoint<Void, AuthTokenResponse> {

    private final AuthenticationService authenticationService;
    private final EventProcessorService eventProcessorService;
    private final AwsStsClient awsStsClient;
    private static final MediaType DEFAULT_MEDIA_TYPE = MediaType.parse("application/json");

    @Inject
    public AuthenticateStsIdentity(AuthenticationService authenticationService,
                                   EventProcessorService eventProcessorService,
                                   AwsStsClient awsStsClient) {

        this.authenticationService = authenticationService;
        this.eventProcessorService = eventProcessorService;
        this.awsStsClient = awsStsClient;
    }

    @Override
    public CompletableFuture<ResponseInfo<AuthTokenResponse>> execute(final RequestInfo<Void> request,
                                                                        final Executor longRunningTaskExecutor,
                                                                        final ChannelHandlerContext ctx) {
        return CompletableFuture.supplyAsync(
                AsyncNettyHelper.supplierWithTracingAndMdc(() -> authenticate(request), ctx),
                longRunningTaskExecutor
        );
    }

    private ResponseInfo<AuthTokenResponse> authenticate(RequestInfo<Void> request) {
        final String headerXAmzDate = getHeaderXAmzDate(request);
        final String headerXAmzSecurityToken = getHeaderXAmzSecurityToken(request);
        final String headerAuthorization = getHeaderAuthorization(request);

        if (headerAuthorization == null || headerXAmzDate == null || headerXAmzSecurityToken == null) {
            throw new ApiException(DefaultApiError.MISSING_AWS_SIGNATURE_HEADERS);
        }

        AwsStsHttpHeader header = new AwsStsHttpHeader(headerXAmzDate, headerXAmzSecurityToken, headerAuthorization);
        GetCallerIdentityResponse getCallerIdentityResponse = awsStsClient.getCallerIdentity(header);
        String iamPrincipalArn = getCallerIdentityResponse.getGetCallerIdentityResult().getArn();
        AuthTokenResponse authResponse = null;
        try {
            authResponse = authenticationService.stsAuthenticate(iamPrincipalArn);
        } catch (ApiException e) {
            eventProcessorService.ingestEvent(auditableEvent(
                    iamPrincipalArn, request, getClass().getSimpleName())
                    .withAction(String.format("Failed to authenticate for reason: %s",
                            String.join(",", e.getApiErrors().stream()
                                    .map(ApiError::getMessage).collect(Collectors.toList()))))
                    .withSuccess(false)
                    .build()
            );
            throw e;
        }

        eventProcessorService.ingestEvent(auditableEvent(
                iamPrincipalArn, request, getClass().getSimpleName())
                .withAction("Successfully authenticated")
                .build()
        );

        return ResponseInfo.newBuilder(authResponse).build();
    }

    @Override
    public Matcher requestMatcher() {
        return Matcher.match("/v2/auth/sts-identity", HttpMethod.POST);
    }
}
