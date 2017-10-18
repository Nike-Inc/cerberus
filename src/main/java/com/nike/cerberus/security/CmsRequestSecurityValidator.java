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

package com.nike.cerberus.security;

import com.nike.backstopper.exception.ApiException;
import com.nike.cerberus.domain.CerberusAuthToken;
import com.nike.cerberus.endpoints.secret.SecureDataEndpointV1;
import com.nike.cerberus.error.DefaultApiError;
import com.nike.cerberus.service.AuthTokenService;
import com.nike.riposte.server.error.validation.RequestSecurityValidator;
import com.nike.riposte.server.http.RequestInfo;
import com.nike.riposte.server.http.Endpoint;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.handler.codec.http.HttpHeaders;
import javax.ws.rs.core.SecurityContext;
import java.net.URI;
import java.util.Collection;
import java.util.Optional;

/**
 * Request validator responsible for validating that the X-Vault-Token or X-Cerberus-Token header is present and valid.
 * The client token entity will also be placed in the request context to be referenced downstream.
 */
public class CmsRequestSecurityValidator implements RequestSecurityValidator {

    public static final String HEADER_X_CERBERUS_TOKEN = "X-Cerberus-Token";
    public static final String LEGACY_AUTH_TOKN_HEADER = "X-Vault-Token";
    public static final String SECURITY_CONTEXT_ATTR_KEY = "cerberusSecurityContext";

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final Collection<Endpoint<?>> endpointsToValidate;
    private final AuthTokenService authTokenService;

    public CmsRequestSecurityValidator(Collection<Endpoint<?>> endpointsToValidate,
                                       AuthTokenService authTokenService) {

        this.endpointsToValidate = endpointsToValidate;
        this.endpointsToValidate.forEach(endpoint -> log.info("auth protected: {}", endpoint.getClass().getName()));

        this.authTokenService = authTokenService;
    }

    @Override
    public void validateSecureRequestForEndpoint(RequestInfo<?> requestInfo, Endpoint<?> endpoint) {
        String token = parseRequiredAuthHeaderFromRequest(requestInfo.getHeaders());

        Optional<CerberusAuthToken> authToken = authTokenService.getCerberusAuthToken(token);

        CerberusPrincipal principal = null;
        if (! authToken.isPresent()) {
            // This is a hack, Secure Data Endpoint V1 endpoints need to honor the Vault API contract and
            // cannot throw backstopper errors, we will handle this in the SecureDataEndpointV1 class
            if (! (endpoint instanceof SecureDataEndpointV1)) {
                throw new ApiException(DefaultApiError.AUTH_TOKEN_INVALID);
            }
        } else {
            principal = new CerberusPrincipal(authToken.get());
        }

        final CerberusSecurityContext securityContext = new CerberusSecurityContext(principal,
                URI.create(requestInfo.getUri()).getScheme());
        requestInfo.addRequestAttribute(SECURITY_CONTEXT_ATTR_KEY, securityContext);
    }

    private String parseRequiredAuthHeaderFromRequest(HttpHeaders headers) {
        final String legacyToken = headers.get(LEGACY_AUTH_TOKN_HEADER);
        final String cerberusToken = headers.get(HEADER_X_CERBERUS_TOKEN);

        if (StringUtils.isBlank(legacyToken) && StringUtils.isBlank(cerberusToken)) {
            throw new ApiException(DefaultApiError.AUTH_TOKEN_INVALID);
        }

        return StringUtils.isNotBlank(legacyToken) ? legacyToken : cerberusToken;
    }

    @Override
    public Collection<Endpoint<?>> endpointsToValidate() {
        return endpointsToValidate;
    }

    /**
     * @return true if this security validator is fast enough that {@link #validateSecureRequestForEndpoint(RequestInfo, * Endpoint)} can run without unnecessarily blocking Netty worker threads to the point it becomes a bottleneck and
     * adversely affecting throughput, false otherwise when {@link #validateSecureRequestForEndpoint(RequestInfo, * Endpoint)} should be run asynchronously off the Netty worker thread. Defaults to true because security validators
     * are usually actively crunching numbers and the cost of context switching to an async thread is often worse than
     * just doing the work on the Netty worker thread. <b>Bottom line: This is affected heavily by numerous factors and
     * your specific use case - you should test under high load with this turned on and off for your security validator
     * and see which one causes better behavior.</b>
     */
    @Override
    public boolean isFastEnoughToRunOnNettyWorkerThread() {
        return false;
    }

    public static Optional<SecurityContext> getSecurityContextForRequest(RequestInfo<?> requestInfo) {
        final Object securityContext = requestInfo.getRequestAttributes().get(SECURITY_CONTEXT_ATTR_KEY);

        if (securityContext == null) {
            return Optional.empty();
        } else {
            return Optional.of((SecurityContext) securityContext);
        }
    }
}
