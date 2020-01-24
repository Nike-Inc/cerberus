/*
 * Copyright (c) 2020 Nike, inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nike.cerberus.controller;

import static org.springframework.web.bind.annotation.RequestMethod.GET;

import com.nike.backstopper.exception.ApiException;
import com.nike.cerberus.domain.SecureDataVersionsResult;
import com.nike.cerberus.error.DefaultApiError;
import com.nike.cerberus.event.filter.AuditLoggingFilterDetails;
import com.nike.cerberus.security.PrincipalHasReadPermsForPath;
import com.nike.cerberus.service.SecureDataVersionService;
import com.nike.cerberus.util.SdbAccessRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/v1/secret-versions")
public class GetSecureDataVersionsController {

  private final SecureDataVersionService secureDataVersionService;
  private final SdbAccessRequest sdbAccessRequest; // Request scoped proxy bean
  private final AuditLoggingFilterDetails auditLoggingFilterDetails;

  @Autowired
  public GetSecureDataVersionsController(
      SecureDataVersionService secureDataVersionService,
      SdbAccessRequest sdbAccessRequest,
      AuditLoggingFilterDetails auditLoggingFilterDetails) {

    this.secureDataVersionService = secureDataVersionService;
    this.sdbAccessRequest = sdbAccessRequest;
    this.auditLoggingFilterDetails = auditLoggingFilterDetails;
  }

  @PrincipalHasReadPermsForPath
  @RequestMapping(value = "/**", method = GET)
  public SecureDataVersionsResult getVersionPathsForSdb(
      @RequestParam(value = "limit", required = false, defaultValue = "100") int limit,
      @RequestParam(value = "offset", required = false, defaultValue = "0") int offset) {

    var result =
        secureDataVersionService.getSecureDataVersionSummariesByPath(
            sdbAccessRequest.getSdbId(),
            sdbAccessRequest.getPath(),
            sdbAccessRequest.getCategory(),
            limit,
            offset);

    if (result.getSecureDataVersionSummaries().isEmpty()) {
      auditLoggingFilterDetails.setAction(
          "Failed to find versions for secret with path: " + sdbAccessRequest.getPath());

      throw ApiException.newBuilder().withApiErrors(DefaultApiError.GENERIC_BAD_REQUEST).build();
    }

    return result;
  }
}
