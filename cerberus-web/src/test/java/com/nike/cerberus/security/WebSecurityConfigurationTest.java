package com.nike.cerberus.security;

import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.web.cors.CorsConfiguration;

public class WebSecurityConfigurationTest {

  @Test
  public void testNoAllowedOriginPattern() {
    WebSecurityConfiguration wsc = new WebSecurityConfiguration();
    CorsConfiguration config = new CorsConfiguration();
    wsc.getConfigurationSource(config);
    Assert.assertEquals(config.getAllowedOriginPatterns(), null);
    Assert.assertEquals(config.getAllowedHeaders(), List.of("*"));
    Assert.assertEquals(config.getAllowedMethods(), List.of("*"));
    Assert.assertFalse(config.getAllowCredentials());
  }

  @Test
  public void testCustomAllowedOriginPattern() {
    WebSecurityConfiguration wsc = new WebSecurityConfiguration();
    wsc.setAllowedOriginPattern("https://*.testdomain.com");
    CorsConfiguration config = new CorsConfiguration();
    wsc.getConfigurationSource(config);
    Assert.assertEquals(config.getAllowedOriginPatterns(), List.of("https://*.testdomain.com"));
  }
}
