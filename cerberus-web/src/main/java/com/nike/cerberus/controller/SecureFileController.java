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

import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.*;

import com.nike.backstopper.exception.ApiException;
import com.nike.cerberus.domain.SecureDataType;
import com.nike.cerberus.domain.SecureFile;
import com.nike.cerberus.error.DefaultApiError;
import com.nike.cerberus.security.PrincipalHasDeletePermsForPath;
import com.nike.cerberus.security.PrincipalHasReadPermsForPath;
import com.nike.cerberus.security.PrincipalHasWritePermsForPath;
import com.nike.cerberus.service.SecureDataService;
import com.nike.cerberus.service.SecureDataVersionService;
import com.nike.cerberus.util.CustomApiError;
import com.nike.cerberus.util.SdbAccessRequest;
import java.io.IOException;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/v1/secure-file")
public class SecureFileController {

  private final SecureDataService secureDataService;
  private final SecureDataVersionService secureDataVersionService;
  private final SdbAccessRequest sdbAccessRequest; // Request scoped proxy bean
  private final Tika tika;

  @Autowired
  public SecureFileController(
      SecureDataService secureDataService,
      SecureDataVersionService secureDataVersionService,
      SdbAccessRequest sdbAccessRequest) {

    this.secureDataService = secureDataService;
    this.secureDataVersionService = secureDataVersionService;
    this.sdbAccessRequest = sdbAccessRequest;
    tika = new Tika();
  }

  @PrincipalHasReadPermsForPath
  @RequestMapping(value = "/**", method = HEAD)
  public ResponseEntity<Void> headSecureFile() {
    var secureFileSummary =
        secureDataService
            .readFileMetadataOnly(sdbAccessRequest.getSdbId(), sdbAccessRequest.getPath())
            .orElseThrow(
                () ->
                    new ApiException.Builder()
                        .withApiErrors(DefaultApiError.ENTITY_NOT_FOUND)
                        .build());

    var mimeType = MediaType.parseMediaType(tika.detect(secureFileSummary.getName()));

    return ResponseEntity.ok()
        .contentType(mimeType)
        .contentLength(secureFileSummary.getSizeInBytes())
        .header(
            "Content-Disposition",
            String.format("attachment; filename=\"%s\"", secureFileSummary.getName()))
        .build();
  }

  @PrincipalHasReadPermsForPath
  @RequestMapping(value = "/**", method = GET)
  public ResponseEntity<?> getSecureFile() {
    var secureFile =
        secureDataService
            .readFile(sdbAccessRequest.getSdbId(), sdbAccessRequest.getPath())
            .orElseThrow(
                () ->
                    new ApiException.Builder()
                        .withApiErrors(DefaultApiError.ENTITY_NOT_FOUND)
                        .build());

    return generateSecureFileResponse(secureFile);
  }

  @PrincipalHasReadPermsForPath
  @RequestMapping(params = "versionId", value = "/**", method = GET)
  public ResponseEntity<?> getSecureFileVersion(
      @RequestParam(value = "versionId") String versionId) {
    var secureFile =
        secureDataVersionService
            .getSecureFileVersionById(
                sdbAccessRequest.getSdbId(),
                versionId,
                sdbAccessRequest.getCategory(),
                sdbAccessRequest.getPath())
            .orElseThrow(
                () ->
                    new ApiException.Builder()
                        .withApiErrors(DefaultApiError.ENTITY_NOT_FOUND)
                        .build());

    return generateSecureFileResponse(secureFile);
  }

  @PrincipalHasWritePermsForPath
  @RequestMapping(
      value = "/**",
      method = {POST, PUT},
      consumes = MULTIPART_FORM_DATA_VALUE)
  public void writeSecureFile(@RequestParam("file-content") MultipartFile file) {
    byte[] fileContents;
    try {
      fileContents = file.getBytes();
    } catch (IOException ex) {
      String msg = "Failed to get contents from multipart file";
      throw ApiException.newBuilder()
          .withExceptionMessage(msg)
          .withExceptionCause(ex)
          .withApiErrors(
              CustomApiError.createCustomApiError(DefaultApiError.GENERIC_BAD_REQUEST, msg))
          .build();
    }

    secureDataService.writeSecureFile(
        sdbAccessRequest.getSdbId(),
        sdbAccessRequest.getPath(),
        fileContents,
        fileContents.length,
        sdbAccessRequest.getPrincipal().getName());
  }

  @PrincipalHasDeletePermsForPath
  @RequestMapping(value = "/**", method = DELETE)
  public void deleteSecureFile() {
    secureDataService.deleteSecret(
        sdbAccessRequest.getSdbId(),
        sdbAccessRequest.getPath(),
        SecureDataType.FILE,
        sdbAccessRequest.getPrincipal().getName());
  }

  private ResponseEntity<?> generateSecureFileResponse(@NotNull SecureFile secureFile) {
    byte[] fileContents = secureFile.getData();
    var mimeType = MediaType.parseMediaType(tika.detect(secureFile.getName()));

    return ResponseEntity.ok()
        .contentType(mimeType)
        .header(
            "Content-Disposition",
            String.format("attachment; filename=\"%s\"", secureFile.getName()))
        .body(fileContents);
  }
}
