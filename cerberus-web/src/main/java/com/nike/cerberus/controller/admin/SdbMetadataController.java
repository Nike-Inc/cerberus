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

package com.nike.cerberus.controller.admin;

import static com.nike.cerberus.security.CerberusPrincipal.ROLE_ADMIN;
import static org.springframework.web.bind.annotation.RequestMethod.*;

import com.nike.cerberus.domain.SDBMetadata;
import com.nike.cerberus.domain.SDBMetadataResult;
import com.nike.cerberus.service.MetadataService;
import com.nike.cerberus.service.SafeDepositBoxService;
import javax.annotation.security.RolesAllowed;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Validated
@RestController
@RolesAllowed(ROLE_ADMIN)
@RequestMapping("/v1/metadata")
public class SdbMetadataController {

  private final MetadataService metadataService;
  private final SafeDepositBoxService safeDepositBoxService;

  @Autowired
  public SdbMetadataController(
      MetadataService metadataService, SafeDepositBoxService safeDepositBoxService) {

    this.metadataService = metadataService;
    this.safeDepositBoxService = safeDepositBoxService;
  }

  @RequestMapping(method = GET)
  public SDBMetadataResult getMetadata(
      @RequestParam(value = "limit", required = false, defaultValue = "100") int limit,
      @RequestParam(value = "offset", required = false, defaultValue = "0") int offset,
      @RequestParam(value = "sdbNameFilter", required = false) String sdbNameFilter) {

    return metadataService.getSDBMetadata(limit, offset, sdbNameFilter);
  }

  @RequestMapping(method = PUT)
  public void restoreSdbIncludingDataInRequest(
      @RequestBody SDBMetadata sdbObject, Authentication authentication) {
    safeDepositBoxService
        .getSafeDepositBoxIdByPath(sdbObject.getPath())
        .ifPresent(safeDepositBoxService::deleteSafeDepositBox);
    var principal = authentication.getName();
    metadataService.restoreMetadata(sdbObject, principal);
  }
}
