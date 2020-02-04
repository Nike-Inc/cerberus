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

import com.nike.cerberus.domain.SecureFileSummaryResult;
import com.nike.cerberus.security.PrincipalHasReadPermsForPath;
import com.nike.cerberus.service.SecureDataService;
import com.nike.cerberus.util.SdbAccessRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/v1/secure-files")
public class SecureFilesSummaryController {

  private final SecureDataService secureDataService;
  private final SdbAccessRequest sdbAccessRequest; // Request scoped proxy bean

  @Autowired
  public SecureFilesSummaryController(
      SecureDataService secureDataService, SdbAccessRequest sdbAccessRequest) {

    this.secureDataService = secureDataService;
    this.sdbAccessRequest = sdbAccessRequest;
  }

  @PrincipalHasReadPermsForPath
  @RequestMapping(value = "/**", method = GET)
  public SecureFileSummaryResult listSecureFiles(
      @RequestParam(value = "limit", required = false, defaultValue = "100") int limit,
      @RequestParam(value = "offset", required = false, defaultValue = "0") int offset) {

    return secureDataService.listSecureFilesSummaries(
        sdbAccessRequest.getSdbId(), sdbAccessRequest.getPath(), limit, offset);
  }
}
