package com.nike.cerberus.util;

import java.security.NoSuchAlgorithmException;
import org.junit.Assert;
import org.junit.Test;

public class AuthTokenGeneratorTest {

  @Test
  public void testAuthTokenGeneratorIfLengthIsConfiguredWrongly() {
    String exceptionMessage = "";
    Exception exception = null;
    try {
      new AuthTokenGenerator(0);
    } catch (NoSuchAlgorithmException | IllegalArgumentException e) {
      exceptionMessage = e.getMessage();
      exception = e;
    }
    Assert.assertNotNull(exception);
    Assert.assertEquals(
        "${cerberus.auth.token.generate.length} must be at least 64 but was 0", exceptionMessage);
  }

  @Test
  public void testAuthTokenGenerateShouldGenerateNewToken() throws NoSuchAlgorithmException {
    AuthTokenGenerator authTokenGenerator = new AuthTokenGenerator(64);
    String secureToken = authTokenGenerator.generateSecureToken();
    Assert.assertNotNull(secureToken);
    Assert.assertEquals(64, secureToken.length());
  }
}
