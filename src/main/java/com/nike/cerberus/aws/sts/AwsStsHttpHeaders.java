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
 */

package com.nike.cerberus.aws.sts;

import com.nike.riposte.server.http.RequestInfo;
import io.netty.handler.codec.http.HttpHeaders;

public final class AwsStsHttpHeaders {

    public static final String HEADER_X_AMZ_DATE = "x-amz-date";
    public static final String HEADER_X_AMZ_SECURITY_TOKEN = "x-amz-security-token";
    public static final String HEADER_AUTHORIZATION = "Authorization";

    /**
     * Get the value of the x-amz-date header
     */
    public static String getHeaderXAmzDate(RequestInfo request) {
        final HttpHeaders headers = request.getHeaders();
        if (headers != null) {
            return headers.get(HEADER_X_AMZ_DATE);
        }
        return null;
    }

    /**
     * Get the value of the x-amz-security-token header
     */
    public static String getHeaderXAmzSecurityToken(RequestInfo request) {
        final HttpHeaders headers = request.getHeaders();
        if (headers != null) {
            return headers.get(HEADER_X_AMZ_SECURITY_TOKEN);
        }
        return null;
    }

    /**
     * Get the value of the Authorization header
     */
    public static String getHeaderAuthorization(RequestInfo request) {
        final HttpHeaders headers = request.getHeaders();
        if (headers != null) {
            return headers.get(HEADER_AUTHORIZATION);
        }
        return null;
    }
}
