package com.nike.cerberus.security;
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

import com.nike.riposte.server.http.RequestInfo;
import com.nike.riposte.server.http.ResponseInfo;
import com.nike.riposte.server.http.filter.RequestAndResponseFilter;
import io.netty.channel.ChannelHandlerContext;

/**
 * Add Strict-Transport-Security HTTP header to inform browser to use HTTPS only.
 * <p>
 * This doesn't seem to break local development over 8080.
 * <p>
 * https://www.owasp.org/index.php/HTTP_Strict_Transport_Security_Cheat_Sheet
 */
public class StrictTransportSecurityRequestAndResponseFilter implements RequestAndResponseFilter {

    private static final String STRICT_TRANSPORT_SECURITY_HEADER_NAME = "Strict-Transport-Security";

    /**
     * Header Value. 31536000 = 1 year
     */
    private static final String STRICT_TRANSPORT_SECURITY_HEADER_VALUE = "max-age=31536000; includeSubDomains";

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> RequestInfo<T> filterRequestFirstChunkNoPayload(RequestInfo<T> currentRequestInfo, ChannelHandlerContext ctx) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> RequestInfo<T> filterRequestLastChunkWithFullPayload(RequestInfo<T> currentRequestInfo, ChannelHandlerContext ctx) {
        return null;
    }

    /**
     * Add Strict-Transport-Security HTTP header to inform browser to use HTTPS only.
     *
     * {@inheritDoc}
     */
    @Override
    public <T> ResponseInfo<T> filterResponse(ResponseInfo<T> currentResponseInfo, RequestInfo<?> requestInfo, ChannelHandlerContext ctx) {
        currentResponseInfo.getHeaders().add(STRICT_TRANSPORT_SECURITY_HEADER_NAME, STRICT_TRANSPORT_SECURITY_HEADER_VALUE);
        return currentResponseInfo;
    }

}
