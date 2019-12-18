package com.nike.cerberus.controller.authentication;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

import com.nike.backstopper.apierror.ApiError;
import com.nike.backstopper.exception.ApiException;
import com.nike.cerberus.domain.DomainConstants;
import com.nike.cerberus.domain.EncryptedAuthDataWrapper;
import com.nike.cerberus.domain.IamRoleCredentials;
import com.nike.cerberus.event.AuditLoggingFilterDetails;
import com.nike.cerberus.service.AuthenticationService;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Validated
@RestController
@RequestMapping("/v1/auth/iam-role")
public class AwsIamKmsAuthV1Controller {

  private final AuthenticationService authenticationService;
  private final AuditLoggingFilterDetails auditLoggingFilterDetails;

  @Autowired
  public AwsIamKmsAuthV1Controller(
      AuthenticationService authenticationService,
      AuditLoggingFilterDetails auditLoggingFilterDetails) {

    this.authenticationService = authenticationService;
    this.auditLoggingFilterDetails = auditLoggingFilterDetails;
  }

  @RequestMapping(method = POST, consumes = APPLICATION_JSON_VALUE)
  public EncryptedAuthDataWrapper authenticate(@RequestBody IamRoleCredentials request) {
    String iamPrincipalArn =
        String.format(
            DomainConstants.AWS_IAM_ROLE_ARN_TEMPLATE,
            request.getAccountId(),
            request.getRoleName());

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
