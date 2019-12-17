package com.nike.cerberus.controller.authentication;

import static com.nike.cerberus.event.AuditUtils.createBaseAuditableEvent;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

import com.nike.backstopper.apierror.ApiError;
import com.nike.backstopper.exception.ApiException;
import com.nike.cerberus.domain.AwsIamKmsAuthRequest;
import com.nike.cerberus.domain.EncryptedAuthDataWrapper;
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
@RequestMapping("/v2/auth/iam-principal")
public class AwsIamKmsAuthV2Controller {

  private final AuthenticationService authenticationService;
  private final EventProcessorService eventProcessorService;

  @Autowired
  public AwsIamKmsAuthV2Controller(
      AuthenticationService authenticationService, EventProcessorService eventProcessorService) {

    this.authenticationService = authenticationService;
    this.eventProcessorService = eventProcessorService;
  }

  @RequestMapping(method = POST)
  public EncryptedAuthDataWrapper authenticate(@RequestBody AwsIamKmsAuthRequest request) {
    EncryptedAuthDataWrapper authResponse;
    try {
      authResponse = authenticationService.authenticate(request);
    } catch (ApiException e) {
      eventProcessorService.ingestEvent(
          createBaseAuditableEvent(request.getIamPrincipalArn())
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
        createBaseAuditableEvent(request.getIamPrincipalArn())
            .withAction(
                String.format("Successfully authenticated in region %s", request.getRegion()))
            .build());

    return authResponse;
  }
}
