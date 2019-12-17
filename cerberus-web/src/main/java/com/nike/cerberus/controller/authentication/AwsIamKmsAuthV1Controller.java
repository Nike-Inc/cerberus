package com.nike.cerberus.controller.authentication;

import static com.nike.cerberus.event.AuditUtils.createBaseAuditableEvent;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

import com.nike.backstopper.apierror.ApiError;
import com.nike.backstopper.exception.ApiException;
import com.nike.cerberus.domain.DomainConstants;
import com.nike.cerberus.domain.EncryptedAuthDataWrapper;
import com.nike.cerberus.domain.IamRoleCredentials;
import com.nike.cerberus.service.AuthenticationService;
import com.nike.cerberus.service.EventProcessorService;
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
  private final EventProcessorService eventProcessorService;

  @Autowired
  public AwsIamKmsAuthV1Controller(
      AuthenticationService authenticationService, EventProcessorService eventProcessorService) {

    this.authenticationService = authenticationService;
    this.eventProcessorService = eventProcessorService;
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
      eventProcessorService.ingestEvent(
          createBaseAuditableEvent(iamPrincipalArn)
              .withAction(
                  String.format(
                      "Failed to authenticate in region %s, for reason: %s",
                      request.getRegion(),
                      e.getApiErrors().stream()
                          .map(ApiError::getMessage)
                          .collect(Collectors.joining(","))))
              .withSuccess(false)
              .build());
      throw e;
    }

    eventProcessorService.ingestEvent(
        createBaseAuditableEvent(iamPrincipalArn)
            .withAction(
                String.format("Successfully authenticated in region %s", request.getRegion()))
            .build());

    return authResponse;
  }
}
