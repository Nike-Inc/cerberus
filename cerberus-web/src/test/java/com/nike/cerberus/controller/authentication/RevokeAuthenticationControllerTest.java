package com.nike.cerberus.controller.authentication;

import com.nike.cerberus.security.CerberusPrincipal;
import com.nike.cerberus.service.AuthenticationService;
import java.time.OffsetDateTime;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class RevokeAuthenticationControllerTest {

  @Mock private AuthenticationService authenticationService;

  @InjectMocks private RevokeAuthenticationController revokeAuthenticationController;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void testRevokeAuthentication() {
    OffsetDateTime now = OffsetDateTime.now();
    CerberusPrincipal cerberusPrincipal = Mockito.mock(CerberusPrincipal.class);
    Mockito.when(cerberusPrincipal.getTokenExpires()).thenReturn(now);
    revokeAuthenticationController.revokeAuthentication(cerberusPrincipal);
    Mockito.verify(authenticationService).revoke(cerberusPrincipal, now);
  }
}
