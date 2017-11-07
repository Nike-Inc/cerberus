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

    @Override
    public <T> RequestInfo<T> filterRequestFirstChunkNoPayload(RequestInfo<T> currentRequestInfo, ChannelHandlerContext ctx) {
        return null;
    }

    @Override
    public <T> RequestInfo<T> filterRequestLastChunkWithFullPayload(RequestInfo<T> currentRequestInfo, ChannelHandlerContext ctx) {
        return null;
    }

    /**
     * Called by the application when the first chunk of the response is about to be sent to the client. Depending on
     * how the response was generated ("normal" endpoint vs proxy/router endpoint for example) this may be a chunked
     * response in which case no response payload would be available, or it might be a full response that has the
     * payload available. You can use {@link ResponseInfo#isChunkedResponse()} to determine if the payload is
     * available.
     *
     * @param currentResponseInfo The current response info, which may or may not have the payload attached (call {@link
     *                            ResponseInfo#isChunkedResponse()} to see if the payload is available - chunked response means no payload).
     * @param requestInfo         The {@link RequestInfo} associated with this request. Useful if your adjustments to response headers are
     *                            dependent on the request that was processed (for example).
     * @param ctx                 The {@link ChannelHandlerContext} associated with this request - unlikely to be used but there if you need
     *                            it.
     * @return The response object that should be sent to the caller. Usually you should just return the same response
     * object you received and make adjustments to headers directly, or if you need to modify something that is normally
     * immutable then you might return a delegate/wrapper object that returns all the data from the original response
     * except for the methods that you want to modify. But ultimately it's up to you what you return so be careful.
     * <b>Null can safely be returned - if null is returned then the original response will continue to be used.</b>
     * <p>
     * <p><b>IMPORTANT NOTE:</b> The implementation class of the return value *must* be the same as the original
     * response object. If the classes differ then an error will be logged and the original response will be used
     * instead. For example, {@link ChunkedResponseInfo} must map to {@link ChunkedResponseInfo}, and {@link
     * FullResponseInfo} must map to {@link FullResponseInfo}. This is another reason it's best to simply adjust the
     * original response when possible.
     */
    @Override
    public <T> ResponseInfo<T> filterResponse(ResponseInfo<T> currentResponseInfo, RequestInfo<?> requestInfo, ChannelHandlerContext ctx) {
        currentResponseInfo.getHeaders().add(STRICT_TRANSPORT_SECURITY_HEADER_NAME, STRICT_TRANSPORT_SECURITY_HEADER_VALUE);
        return currentResponseInfo;
    }

}
