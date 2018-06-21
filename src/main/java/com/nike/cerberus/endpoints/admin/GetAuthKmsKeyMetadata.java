package com.nike.cerberus.endpoints.admin;

import com.nike.cerberus.domain.AuthKmsKeyMetadataResult;
import com.nike.cerberus.endpoints.AdminStandardEndpoint;
import com.nike.cerberus.service.KmsService;
import com.nike.riposte.server.http.RequestInfo;
import com.nike.riposte.server.http.ResponseInfo;
import com.nike.riposte.server.http.impl.FullResponseInfo;
import com.nike.riposte.util.AsyncNettyHelper;
import com.nike.riposte.util.Matcher;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpMethod;

import javax.inject.Inject;
import javax.ws.rs.core.SecurityContext;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Endpoint for retrieving kms key metadata for all created keys in the db
 */
public class GetAuthKmsKeyMetadata extends AdminStandardEndpoint<Void, AuthKmsKeyMetadataResult> {

    private final KmsService kmsService;

    @Inject
    public GetAuthKmsKeyMetadata(KmsService kmsService) {
        this.kmsService = kmsService;
    }

    @Override
    public CompletableFuture<ResponseInfo<AuthKmsKeyMetadataResult>> doExecute(RequestInfo<Void> request,
                                                                        Executor longRunningTaskExecutor,
                                                                        ChannelHandlerContext ctx,
                                                                        SecurityContext securityContext) {

        return CompletableFuture.supplyAsync(
                AsyncNettyHelper.supplierWithTracingAndMdc(() -> getAuthKmsKeyMetadata(request), ctx),
                longRunningTaskExecutor
        );
    }

    private FullResponseInfo<AuthKmsKeyMetadataResult> getAuthKmsKeyMetadata(RequestInfo<Void> request) {
        return ResponseInfo.newBuilder(new AuthKmsKeyMetadataResult(
            kmsService.getAuthenticationKmsMetadata()
        )).build();
    }

    @Override
    public Matcher requestMatcher()  {
        return Matcher.match("/v1/admin/authentication-kms-metadata", HttpMethod.GET);
    }
}
