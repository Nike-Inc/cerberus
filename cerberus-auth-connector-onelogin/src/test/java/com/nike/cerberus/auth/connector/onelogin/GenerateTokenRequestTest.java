package com.nike.cerberus.auth.connector.onelogin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class GenerateTokenRequestTest {

  @Test
  public void test_getGrantType() {
    assertEquals("client_credentials", new GenerateTokenRequest().getGrantType());
  }

  @Test
  public void test_setGrantType() {
    assertEquals("foo", new GenerateTokenRequest().setGrantType("foo").getGrantType());
  }

  @Test
  public void test_equals() {
    assertEquals(new GenerateTokenRequest(), new GenerateTokenRequest());
  }

  @Test
  public void test_hashCode() {
    assertEquals(new GenerateTokenRequest().hashCode(), new GenerateTokenRequest().hashCode());
    assertTrue(
        new GenerateTokenRequest().hashCode()
            != new GenerateTokenRequest().setGrantType("foo").hashCode());
  }
}
