package com.nike.cerberus.config;

import com.amazonaws.encryptionsdk.AwsCrypto;
import org.junit.Assert;
import org.junit.Test;

public class ApplicationConfigurationTest {
  /** Test of awsCrypto to placate a code coverage tool */
  @Test
  public void testAwsCryptoBuilder() {
    ApplicationConfiguration appConfig = new ApplicationConfiguration();
    AwsCrypto awsCrypto = appConfig.awsCrypto();
    Assert.assertNotNull(awsCrypto);
  }
}
