package com.nike.cerberus.controller.authentication;

import com.nike.backstopper.exception.ApiException;
import com.nike.cerberus.aws.sts.AwsStsClient;
import com.nike.cerberus.aws.sts.AwsStsHttpHeader;
import com.nike.cerberus.aws.sts.GetCallerIdentityResponse;
import com.nike.cerberus.domain.AuthTokenResponse;
import com.nike.cerberus.error.DefaultApiError;
import com.nike.cerberus.service.AuthenticationService;
import com.nike.cerberus.service.EventProcessorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.nike.cerberus.event.AuditUtils.createBaseAuditableEvent;
import static org.springframework.web.bind.annotation.RequestMethod.*;

@Slf4j
@Validated
@RestController
@RequestMapping("/v2/auth/sts-identity")
public class AwsIamStsAuthController {

  private static final String HEADER_X_AMZ_DATE = "x-amz-date";
  private static final String HEADER_X_AMZ_SECURITY_TOKEN = "x-amz-security-token";
  private static final String HEADER_AUTHORIZATION = "Authorization";

  private final AuthenticationService authenticationService;
  private final EventProcessorService eventProcessorService;
  private final AwsStsClient awsStsClient;

  @Autowired
  public AwsIamStsAuthController(AuthenticationService authenticationService,
                                 EventProcessorService eventProcessorService,
                                 AwsStsClient awsStsClient) {

    this.authenticationService = authenticationService;
    this.eventProcessorService = eventProcessorService;
    this.awsStsClient = awsStsClient;
  }

  @RequestMapping(method = POST)
  public AuthTokenResponse authenticate(@RequestHeader(value = HEADER_X_AMZ_DATE, required = false) String headerXAmzDate, // TODO should we make this required = true?
                                        @RequestHeader(value = HEADER_X_AMZ_SECURITY_TOKEN, required = false) String headerXAmzSecurityToken,  // TODO should we make this required = true?
                                        @RequestHeader(value = HEADER_AUTHORIZATION, required = false) String headerAuthorization) {

    String iamPrincipalArn = null;
    AuthTokenResponse authResponse;
    try {
      if (headerAuthorization == null || headerXAmzDate == null || headerXAmzSecurityToken == null) {
        throw new ApiException(DefaultApiError.MISSING_AWS_SIGNATURE_HEADERS);
      }

      AwsStsHttpHeader header = new AwsStsHttpHeader(headerXAmzDate, headerXAmzSecurityToken, headerAuthorization);
      GetCallerIdentityResponse getCallerIdentityResponse = awsStsClient.getCallerIdentity(header);
      iamPrincipalArn = getCallerIdentityResponse.getGetCallerIdentityResult().getArn();

      authResponse = authenticationService.stsAuthenticate(iamPrincipalArn);
    } catch (Exception e) {
      eventProcessorService.ingestEvent(createBaseAuditableEvent(
        iamPrincipalArn) // TODO this arn will always be null
        .withAction("Failed to authenticate with AWS IAM STS Auth")
        .withSuccess(false)
        .build()
      );
      throw e; // TODO, throw a Backstopper error here
    }

    eventProcessorService.ingestEvent(createBaseAuditableEvent(
      iamPrincipalArn)
      .withAction("Successfully authenticated with AWS IAM STS Auth")
      .build()
    );

    return authResponse;
  }

}
