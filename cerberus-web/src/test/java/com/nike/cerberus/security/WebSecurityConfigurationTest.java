package com.nike.cerberus.security;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;

public class WebSecurityConfigurationTest {

  @InjectMocks private WebSecurityConfiguration webSecurityConfiguration;
  private WebSecurity webSecurity;
  @Mock private AuthenticationManagerBuilder authenticationManagerBuilder;
  @Mock private ObjectPostProcessor<Object> objectPostProcessor;
  @Mock AuthenticationManagerBuilder authenticationBuilder;

  private HttpSecurity httpSecurity;

  @Before
  public void setUp() {
    Map<Class<?>, Object> sharedObjects = new HashMap<>();
    MockitoAnnotations.initMocks(this);
    webSecurity = new WebSecurity(objectPostProcessor);
    httpSecurity = new HttpSecurity(objectPostProcessor, authenticationBuilder, sharedObjects);
  }

  @Test
  public void testgetDoesRequestsRequireAuthMatcher() {
    assertNotNull(webSecurityConfiguration.getDoesRequestsRequireAuthMatcher());
  }

  @Test
  public void testConfigure() {
    try {
      webSecurityConfiguration.configure(webSecurity);
    } catch (Exception exception) {
      assertNull(exception);
    }
  }

  @Test
  public void testConfigureAuthenticationManagerBuilder() {
    try {
      webSecurityConfiguration.configure(authenticationManagerBuilder);
    } catch (Exception exception) {
      assertNull(exception);
    }
  }

  @Test
  public void test() throws Exception {
    try {
      webSecurityConfiguration.configure(httpSecurity);
    } catch (Exception exception) {
      assertNull(exception);
    }
  }
}
