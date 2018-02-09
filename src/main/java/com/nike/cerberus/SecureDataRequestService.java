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

package com.nike.cerberus;

import com.nike.cerberus.domain.SecureDataRequestInfo;
import com.nike.cerberus.security.CerberusPrincipal;
import com.nike.cerberus.security.CmsRequestSecurityValidator;
import com.nike.cerberus.service.PermissionsService;
import com.nike.cerberus.service.SafeDepositBoxService;
import com.nike.riposte.server.http.RequestInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.core.SecurityContext;
import java.util.Optional;

import static org.apache.commons.lang3.StringUtils.isBlank;

public class SecureDataRequestService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final SafeDepositBoxService safeDepositBoxService;

    private final PermissionsService permissionsService;


    @Inject
    public SecureDataRequestService(SafeDepositBoxService safeDepositBoxService,
                                    PermissionsService permissionsService) {
        this.safeDepositBoxService = safeDepositBoxService;
        this.permissionsService = permissionsService;
    }

    public SecureDataRequestInfo parseAndValidateRequest(RequestInfo requestInfo) throws IllegalArgumentException {
        final Optional<SecurityContext> securityContext =
                CmsRequestSecurityValidator.getSecurityContextForRequest(requestInfo);

        if (! securityContext.isPresent() || ! (securityContext.get().getUserPrincipal() instanceof CerberusPrincipal)) {
            log.error("Security context was null or principal was not instance of Cerberus Principal");
            throw new IllegalArgumentException("Security context is invalid.");
        }

        SecureDataRequestInfo info = parseRequestPathInfo(requestInfo.getPath());

        if (isBlank(info.getCategory()) || isBlank(info.getSdbSlug())) {
            log.error("Required path params missing, PATH: {}", requestInfo.getPath());
            throw new IllegalArgumentException("Request path is invalid.");
        }

        CerberusPrincipal principal = (CerberusPrincipal) securityContext.get().getUserPrincipal();
        String sdbBasePath = String.format("%s/%s/", info.getCategory(), info.getSdbSlug());
        SecureDataAction secureDataAction = SecureDataAction.fromMethod(requestInfo.getMethod());

        Optional<String> sdbId = safeDepositBoxService.getSafeDepositBoxIdByPath(sdbBasePath);
        if (! sdbId.isPresent() || ! permissionsService.doesPrincipalHavePermission(principal, sdbId.get(), secureDataAction)) {

            log.error("SDB ID not found or permission was not granted for principal, principal: {}, sdb: {}, path: {}",
                    principal.getName(), sdbBasePath, requestInfo.getPath());
            throw new IllegalArgumentException("Could not find SDB with path: " + sdbBasePath);
        }

        info.setPrincipal(principal);
        info.setSdbId(sdbId.get());

        return info;
    }

    public SecureDataRequestInfo parseRequestPathInfo(String requestPath) {
        final SecureDataRequestInfo info = new SecureDataRequestInfo();

        String[] parts = requestPath
                .replace("//", "/")
                .split("/", 6);

        if (parts.length >= 4) {
            info.setCategory(parts[3]);
        }
        if (parts.length >= 5) {
            info.setSdbSlug(parts[4]);
        }

        if (parts.length >= 6) {
            info.setSubPath(parts[5]);
        }

        return info;
    }
}
