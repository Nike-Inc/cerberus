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

package com.nike.cerberus.controller.authentication;

import static org.springframework.util.MimeTypeUtils.ALL_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nike.backstopper.apierror.ApiError;
import com.nike.backstopper.exception.ApiException;
import com.nike.cerberus.domain.AwsIamKmsAuthRequest;
import com.nike.cerberus.domain.EncryptedAuthDataWrapper;
import com.nike.cerberus.event.filter.AuditLoggingFilterDetails;
import com.nike.cerberus.service.AuthenticationService;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import javax.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Deprecated
@ConditionalOnProperty("cerberus.deprecatedEndpoints.iamKmsAuth.v2.enabled")
@RestController
@RequestMapping("/v2/auth/iam-principal")
public class AwsIamKmsAuthV2Controller {

  private final AuthenticationService authenticationService;
  private final AuditLoggingFilterDetails auditLoggingFilterDetails;
  private final ObjectMapper objectMapper;
  private final Validator validator;

  @Autowired
  public AwsIamKmsAuthV2Controller(
      AuthenticationService authenticationService,
      AuditLoggingFilterDetails auditLoggingFilterDetails,
      ObjectMapper objectMapper,
      Validator validator) {

    this.authenticationService = authenticationService;
    this.auditLoggingFilterDetails = auditLoggingFilterDetails;
    this.objectMapper = objectMapper;
    this.validator = validator;
  }

  @RequestMapping(method = POST, consumes = ALL_VALUE)
  public EncryptedAuthDataWrapper authenticate(HttpEntity<String> httpEntity)
      throws JsonProcessingException {
    var content = httpEntity.getBody();
    if (content == null) {
      throw new RuntimeException("There was an error deserializing the request, the body was null");
    }

    var type = httpEntity.getHeaders().getContentType();
    if (type == null || !type.toString().contains("json")) {
      content = URLDecoder.decode(content, StandardCharsets.UTF_8);
    }

    var request = objectMapper.readValue(content, AwsIamKmsAuthRequest.class);
    validator.validate(request);

    EncryptedAuthDataWrapper authResponse;

    try {
      authResponse = authenticationService.authenticate(request);
    } catch (ApiException e) {
      auditLoggingFilterDetails.setAction(
          String.format(
              "Failed to authenticate in region %s, for reason: %s",
              request.getRegion(),
              e.getApiErrors().stream()
                  .map(ApiError::getMessage)
                  .collect(Collectors.joining(","))));
      throw e;
    }
    auditLoggingFilterDetails.setAction(
        String.format("Successfully authenticated in region %s", request.getRegion()));

    return authResponse;
  }
}
