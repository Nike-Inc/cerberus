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
import com.nike.cerberus.error.DefaultApiError;
import com.nike.vault.client.VaultAdminClient;
import com.nike.vault.client.VaultClientException;
import com.nike.vault.client.VaultServerException;
import com.nike.vault.client.model.VaultClientTokenResponse;
import com.nike.riposte.server.error.validation.RequestSecurityValidator;
import com.nike.riposte.server.http.RequestInfo;
import com.nike.riposte.server.http.Endpoint;
import org.apache.commons.lang3.StringUtils;

import javax.ws.rs.core.SecurityContext;
import java.net.URI;
import java.util.Collection;
import java.util.Optional;

/**
 * Request validator responsible for validating that the X-Vault-Token header is present and valid.
 * The client token entity will also be placed in the request context to be referenced downstream.
 */
public class CmsRequestSecurityValidator implements RequestSecurityValidator {

    public static final String HEADER_X_VAULT_TOKEN = "X-Vault-Token";

    public static final String SECURITY_CONTEXT_ATTR_KEY = "vaultSecurityContext";

    private final Collection<Endpoint<?>> endpointsToValidate;

    private final VaultAdminClient vaultAdminClient;

    public CmsRequestSecurityValidator(final Collection<Endpoint<?>> endpointsToValidate,
                                       final VaultAdminClient vaultAdminClient) {
        this.endpointsToValidate = endpointsToValidate;
        this.vaultAdminClient = vaultAdminClient;
    }

    @Override
    public void validateSecureRequestForEndpoint(RequestInfo<?> requestInfo, Endpoint<?> endpoint) {
        final String vaultToken = requestInfo.getHeaders().get(HEADER_X_VAULT_TOKEN);

        if (StringUtils.isBlank(vaultToken)) {
            throw new ApiException(DefaultApiError.AUTH_VAULT_TOKEN_INVALID);
        }

        try {
            final VaultClientTokenResponse clientTokenResponse = vaultAdminClient.lookupToken(vaultToken);

            final VaultAuthPrincipal principal = new VaultAuthPrincipal(clientTokenResponse);
            final VaultSecurityContext securityContext = new VaultSecurityContext(principal,
                    URI.create(requestInfo.getUri()).getScheme());
            requestInfo.addRequestAttribute(SECURITY_CONTEXT_ATTR_KEY, securityContext);
        } catch (VaultServerException vse) {
            throw ApiException.newBuilder()
                    .withApiErrors(DefaultApiError.AUTH_VAULT_TOKEN_INVALID)
                    .withExceptionCause(vse)
                    .build();
        } catch (VaultClientException vce) {
            throw ApiException.newBuilder()
                    .withApiErrors(DefaultApiError.SERVICE_UNAVAILABLE)
                    .withExceptionCause(vce)
                    .build();
        }
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
