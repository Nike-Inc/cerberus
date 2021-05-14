package com.nike.cerberus.security;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.security.core.Authentication;

public class CerberusPrincipalAuthenticationProviderTest {

  @Test
  public void testAuthenticateShouldReturnSameAuthentication() {
    CerberusPrincipalAuthenticationProvider cerberusPrincipalAuthenticationProvider =
        new CerberusPrincipalAuthenticationProvider();
    Authentication authentication = Mockito.mock(Authentication.class);
    Authentication actualAuthentication =
        cerberusPrincipalAuthenticationProvider.authenticate(authentication);
    Assert.assertEquals(authentication, actualAuthentication);
  }

  @Test
  public void testSupportsShouldReturnFalseIfAuthenticationProviderIsDifferent() {
    CerberusPrincipalAuthenticationProvider cerberusPrincipalAuthenticationProvider =
        new CerberusPrincipalAuthenticationProvider();
    boolean supports =
        cerberusPrincipalAuthenticationProvider.supports(
            CerberusPrincipalAuthenticationProviderTest.class);
    Assert.assertFalse(supports);
  }

  @Test
  public void testSupportsShouldReturnTrueIfAuthenticationProviderIsCerberus() {
    CerberusPrincipalAuthenticationProvider cerberusPrincipalAuthenticationProvider =
        new CerberusPrincipalAuthenticationProvider();
    boolean supports =
        cerberusPrincipalAuthenticationProvider.supports(
            CerberusPrincipalAuthenticationProvider.class);
    Assert.assertTrue(supports);
  }
}
