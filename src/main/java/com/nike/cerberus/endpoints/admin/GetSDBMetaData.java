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

import com.google.inject.Inject;
import com.nike.backstopper.apierror.ApiErrorBase;
import com.nike.backstopper.exception.ApiException;
import com.nike.cerberus.domain.SDBMetaDataResult;
import com.nike.cerberus.endpoints.AdminStandardEndpoint;
import com.nike.cerberus.error.DefaultApiError;
import com.nike.cerberus.service.MetaDataService;
import com.nike.riposte.server.http.RequestInfo;
import com.nike.riposte.server.http.ResponseInfo;
import com.nike.riposte.util.Matcher;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpMethod;
import org.apache.commons.lang3.StringUtils;

import javax.ws.rs.core.SecurityContext;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Returns meta data for all SDBs in CMS
 */
public class GetSDBMetaData extends AdminStandardEndpoint<Void, SDBMetaDataResult> {

    protected static final String OFFSET_QUERY_KEY = "offset";
    protected static final String LIMIT_QUERY_KEY = "limit";
    protected static final int DEFAULT_OFFSET = 0;
    protected static final int DEFAULT_LIMIT = 100;

    private final MetaDataService metaDataService;

    @Inject
    public GetSDBMetaData(MetaDataService metaDataService) {
        this.metaDataService = metaDataService;
    }

    @SuppressWarnings("ConstantConditions") // it lies
    @Override
    public CompletableFuture<ResponseInfo<SDBMetaDataResult>> doExecute(final RequestInfo<Void> request,
                                                                        final Executor longRunningTaskExecutor,
                                                                        final ChannelHandlerContext ctx,
                                                                        final SecurityContext securityContext) {

        return CompletableFuture.supplyAsync(
                () -> ResponseInfo.newBuilder(metaDataService.getSDBMetaData(getLimit(request), getOffset(request)))
                        .build(), longRunningTaskExecutor
        );
    }

    /**
     * Parses and validates limit query param
     * @param request The request
     * @return default or parsed vaule
     */
    protected int getLimit(RequestInfo<Void> request) {
        String limitQueryValue = request.getQueryParamSingle(LIMIT_QUERY_KEY);
        int limit = DEFAULT_LIMIT;

        if (limitQueryValue != null) {
            validateLimitQuery(limitQueryValue);
            limit = Integer.parseInt(limitQueryValue);
        }

        return limit;
    }

    /**
     * validates that the limit query is a valid number >= 1
     * @param limitQueryValue
     */
    protected void validateLimitQuery(String limitQueryValue) {
        if (! StringUtils.isNumeric(limitQueryValue) || Integer.parseInt(limitQueryValue) < 1) {
            throw ApiException.newBuilder()
                    .withApiErrors(new ApiErrorBase(
                            DefaultApiError.INVALID_QUERY_PARAMS.getName(),
                            DefaultApiError.INVALID_QUERY_PARAMS.getErrorCode(),
                            String.format("limit query param must be an int >= 1, '%s' given", limitQueryValue),
                            DefaultApiError.INVALID_QUERY_PARAMS.getHttpStatusCode()
                    )).build();
        }
    }

    /**
     * Parses and validates offset query param
     * @param request The request
     * @return default or parsed vaule
     */
    protected int getOffset(RequestInfo<Void> request) {
        String offsetQueryValue = request.getQueryParamSingle(OFFSET_QUERY_KEY);
        int offset = DEFAULT_OFFSET;

        if (offsetQueryValue != null) {
            validateOffsetQuery(offsetQueryValue);
            offset = Integer.parseInt(offsetQueryValue);
        }

        return offset;
    }

    /**
     * Validates that the offset value is a number that is >= 0
     * @param offsetQueryValue
     */
    protected void validateOffsetQuery(String offsetQueryValue) {
        if (! StringUtils.isNumeric(offsetQueryValue)) {
            throw ApiException.newBuilder()
                    .withApiErrors(new ApiErrorBase(
                            DefaultApiError.INVALID_QUERY_PARAMS.getName(),
                            DefaultApiError.INVALID_QUERY_PARAMS.getErrorCode(),
                            String.format("offset query param must be an int >= 0, '%s' given", offsetQueryValue),
                            DefaultApiError.INVALID_QUERY_PARAMS.getHttpStatusCode()
                    )).build();
        }
    }

    @Override
    public Matcher requestMatcher() {
        return Matcher.match("/v1/metadata", HttpMethod.GET);
    }

}
