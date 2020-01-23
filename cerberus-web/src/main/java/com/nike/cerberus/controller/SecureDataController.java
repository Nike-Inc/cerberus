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

import static org.springframework.util.MimeTypeUtils.ALL_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nike.cerberus.domain.*;
import com.nike.cerberus.security.CerberusPrincipal;
import com.nike.cerberus.security.PrincipalHasDeletePermsForPath;
import com.nike.cerberus.security.PrincipalHasReadPermsForPath;
import com.nike.cerberus.security.PrincipalHasWritePermsForPath;
import com.nike.cerberus.service.SecureDataService;
import com.nike.cerberus.service.SecureDataVersionService;
import com.nike.cerberus.util.SdbAccessRequest;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Validated
@RestController
@RequestMapping("/v1/secret")
public class SecureDataController {

  private final SecureDataService secureDataService;
  private final SecureDataVersionService secureDataVersionService;
  private final SdbAccessRequest sdbAccessRequest; // Request scoped proxy bean

  @Autowired
  public SecureDataController(
      SecureDataService secureDataService,
      SecureDataVersionService secureDataVersionService,
      SdbAccessRequest sdbAccessRequest) {

    this.secureDataService = secureDataService;
    this.secureDataVersionService = secureDataVersionService;
    this.sdbAccessRequest = sdbAccessRequest;
  }

  @PrincipalHasReadPermsForPath
  @RequestMapping(value = "/**", method = GET)
  public ResponseEntity<?> readSecureData() {
    Optional<SecureData> secureDataOpt =
        secureDataService.readSecret(sdbAccessRequest.getSdbId(), sdbAccessRequest.getPath());
    return ResponseEntity.of(
        secureDataOpt.map(
            secureData -> {
              var data = secureData.getData();
              var metadata = secureDataService.parseSecretMetadata(secureData);
              return generateSecureDataResponse(data, metadata);
            }));
  }

  @PrincipalHasReadPermsForPath
  @RequestMapping(params = "list", value = "/**", method = GET)
  public ResponseEntity<?> listKeys(@RequestParam(value = "list") String list) {

    if (!Boolean.parseBoolean(list)) {
      return readSecureData(); // TODO
    }

    Set<String> keys =
        secureDataService.listKeys(sdbAccessRequest.getSdbId(), sdbAccessRequest.getPath());
    return ResponseEntity.of(
        Optional.of(SecureDataResponse.builder().data(Map.of("keys", keys)).build()));
  }

  @PrincipalHasReadPermsForPath
  @RequestMapping(params = "versionId", value = "/**", method = GET)
  public ResponseEntity<?> readSecureDataVersion(
      @RequestParam(value = "versionId") String versionId) {

    Optional<SecureDataVersion> secureDataVersionOpt =
        secureDataVersionService.getSecureDataVersionById(
            sdbAccessRequest.getSdbId(),
            versionId,
            sdbAccessRequest.getCategory(),
            sdbAccessRequest.getPath());

    return ResponseEntity.of(
        secureDataVersionOpt.map(
            secureDataVersion -> {
              var data = secureDataVersion.getData();
              var metadata = secureDataVersionService.parseVersionMetadata(secureDataVersion);
              return generateSecureDataResponse(data, metadata);
            }));
  }

  @PrincipalHasWritePermsForPath
  @RequestMapping(
      value = "/**",
      method = {POST, PUT},
      consumes = ALL_VALUE)
  public void writeSecureData(HttpEntity<String> httpEntity) {
    CerberusPrincipal principal = sdbAccessRequest.getPrincipal();
    secureDataService.writeSecret(
        sdbAccessRequest.getSdbId(),
        sdbAccessRequest.getPath(),
        Optional.ofNullable(httpEntity.getBody())
            .orElseThrow(() -> new RuntimeException("The body must not be null")),
        principal.getName());
  }

  @PrincipalHasDeletePermsForPath
  @RequestMapping(value = "/**", method = DELETE)
  public void deleteSecureData() {
    secureDataService.deleteSecret(
        sdbAccessRequest.getSdbId(),
        sdbAccessRequest.getPath(),
        SecureDataType.OBJECT,
        sdbAccessRequest.getPrincipal().getName());
  }

  private SecureDataResponse generateSecureDataResponse(
      String secureData, Map<String, String> metadata) {
    SecureDataResponse response = new SecureDataResponse();
    response.setRequestId(UUID.randomUUID().toString());
    response.setMetadata(metadata);

    try {
      response.setData(new ObjectMapper().readTree(secureData));
    } catch (IOException e) {
      log.error("Failed to deserialize stored data", e);
    }

    return response;
  }
}
