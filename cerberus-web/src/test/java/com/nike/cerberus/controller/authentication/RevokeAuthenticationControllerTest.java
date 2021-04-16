package com.nike.cerberus.controller.authentication;

import com.nike.cerberus.security.CerberusPrincipal;
import com.nike.cerberus.service.AuthenticationService;
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
    CerberusPrincipal cerberusPrincipal = Mockito.mock(CerberusPrincipal.class);
    Mockito.when(cerberusPrincipal.getToken()).thenReturn("token");
    revokeAuthenticationController.revokeAuthentication(cerberusPrincipal);
    Mockito.verify(authenticationService).revoke("token");
  }
}
