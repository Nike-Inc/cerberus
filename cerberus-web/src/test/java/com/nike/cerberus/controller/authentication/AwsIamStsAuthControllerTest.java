package com.nike.cerberus.controller.authentication;

import com.amazonaws.services.securitytoken.model.GetCallerIdentityResult;
import com.nike.backstopper.exception.ApiException;
import com.nike.cerberus.aws.sts.AwsStsClient;
import com.nike.cerberus.aws.sts.AwsStsHttpHeader;
import com.nike.cerberus.aws.sts.GetCallerIdentityResponse;
import com.nike.cerberus.domain.AuthTokenResponse;
import com.nike.cerberus.error.DefaultApiError;
import com.nike.cerberus.event.filter.AuditLoggingFilterDetails;
import com.nike.cerberus.service.AuthenticationService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class AwsIamStsAuthControllerTest {

  @Mock private AuthenticationService authenticationService;
  @Mock private AwsStsClient awsStsClient;
  @Mock private AuditLoggingFilterDetails auditLoggingFilterDetails;
  @InjectMocks private AwsIamStsAuthController awsIamStsAuthController;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @SuppressFBWarnings
  public void testAuthenticateIfHeaderAmzDateIsNull() {
    ApiException apiException = null;
    try {
      awsIamStsAuthController.authenticate(null, null, null);
    } catch (ApiException e) {
      apiException = e;
    }
    Assert.assertEquals(
        DefaultApiError.MISSING_AWS_SIGNATURE_HEADERS, apiException.getApiErrors().get(0));
  }

  @Test
  @SuppressFBWarnings
  public void testAuthenticateIfHeaderAmzSecurityTokenIsNull() {
    ApiException apiException = null;
    try {
      awsIamStsAuthController.authenticate("date", null, null);
    } catch (ApiException e) {
      apiException = e;
    }
    Assert.assertEquals(
        DefaultApiError.MISSING_AWS_SIGNATURE_HEADERS, apiException.getApiErrors().get(0));
  }

  @Test
  public void testAuthenticate() {
    GetCallerIdentityResponse getCallerIdentityResponse =
        Mockito.mock(GetCallerIdentityResponse.class);
    GetCallerIdentityResult getCallerIdentityResult = Mockito.mock(GetCallerIdentityResult.class);
    Mockito.when(getCallerIdentityResponse.getGetCallerIdentityResult())
        .thenReturn(getCallerIdentityResult);
    Mockito.when(getCallerIdentityResult.getArn()).thenReturn("arn");
    Mockito.when(awsStsClient.getCallerIdentity(Mockito.any(AwsStsHttpHeader.class)))
        .thenReturn(getCallerIdentityResponse);
    AuthTokenResponse authTokenResponse = Mockito.mock(AuthTokenResponse.class);
    Mockito.when(authenticationService.stsAuthenticate("arn")).thenReturn(authTokenResponse);
    AuthTokenResponse actualAuthTokenResponse =
        awsIamStsAuthController.authenticate("date", "token", "authorization");
    Assert.assertSame(authTokenResponse, actualAuthTokenResponse);
    Mockito.verify(auditLoggingFilterDetails)
        .setAction("Successfully authenticated with AWS IAM STS Auth");
  }

  @Test
  public void testAuthenticateWhenSTSAuthenticateThrowsException() {
    GetCallerIdentityResponse getCallerIdentityResponse =
        Mockito.mock(GetCallerIdentityResponse.class);
    GetCallerIdentityResult getCallerIdentityResult = Mockito.mock(GetCallerIdentityResult.class);
    Mockito.when(getCallerIdentityResponse.getGetCallerIdentityResult())
        .thenReturn(getCallerIdentityResult);
    Mockito.when(getCallerIdentityResult.getArn()).thenReturn("arn");
    Mockito.when(awsStsClient.getCallerIdentity(Mockito.any(AwsStsHttpHeader.class)))
        .thenReturn(getCallerIdentityResponse);
    RuntimeException runtimeException = new RuntimeException();
    Mockito.when(authenticationService.stsAuthenticate("arn")).thenThrow(runtimeException);
    RuntimeException actualException = null;
    try {
      awsIamStsAuthController.authenticate("date", "token", "authorization");
    } catch (RuntimeException e) {
      actualException = e;
    }
    Assert.assertSame(runtimeException, actualException);
    String auditMessage = String.format("Failed to authenticate with AWS IAM STS Auth: %s", actualException.getMessage());
    Mockito.verify(auditLoggingFilterDetails, Mockito.atLeastOnce())
        .setAction(auditMessage);
  }
}
