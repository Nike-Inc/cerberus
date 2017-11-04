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

package com.nike.cerberus.security;

import io.netty.handler.codec.http.DefaultHttpHeaders;

/**
 * Some standard security headers for the Dashboard
 */
public class SecurityHttpHeaders extends DefaultHttpHeaders {

    /**
     * Don't allow framing
     */
    private static final String X_FRAME_OPTIONS_HEADER_NAME = "X-Frame-Options";
    private static final String X_FRAME_OPTIONS_HEADER_VALUE = "DENY";

    /**
     * Force browser to stick with declared MIME type
     */
    private static final String X_CONTENT_TYPE_OPTIONS_HEADER_NAME = "X-Content-Type-Options";
    private static final String X_CONTENT_TYPE_OPTIONS_HEADER_VALUE = "nosniff";

    /**
     * XSS protection header
     */
    private static final String X_XSS_PROTECTION_HEADER_NAME = "X-XSS-Protection";
    private static final String X_XSS_PROTECTION_HEADER_VALUE = "1; mode=block";

    /**
     * The Content-Security-Policy header helps protect against XSS and other code injection attacks
     * <p>
     * https://www.owasp.org/index.php/Content_Security_Policy
     * https://en.wikipedia.org/wiki/Content_Security_Policy
     */
    private static final String CONTENT_SECURITY_POLICY_HEADER_NAME = "Content-Security-Policy";
    private static final String CONTENT_SECURITY_POLICY_HEADER_VALUE = "default-src 'none'; connect-src 'self'; font-src https://web.nike.com; img-src 'self'; script-src 'self'; style-src 'unsafe-inline' https://web.nike.com/; frame-ancestors 'none';";

    /**
     * Some standard security headers for the Dashboard
     */
    public SecurityHttpHeaders() {
        this.add(X_FRAME_OPTIONS_HEADER_NAME, X_FRAME_OPTIONS_HEADER_VALUE);
        this.add(X_CONTENT_TYPE_OPTIONS_HEADER_NAME, X_CONTENT_TYPE_OPTIONS_HEADER_VALUE);
        this.add(X_XSS_PROTECTION_HEADER_NAME, X_XSS_PROTECTION_HEADER_VALUE);
        this.add(CONTENT_SECURITY_POLICY_HEADER_NAME, CONTENT_SECURITY_POLICY_HEADER_VALUE);
    }

}
